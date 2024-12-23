package stocks.api;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.inject.Inject;
import stocks.models.GameData;
import stocks.models.HistoricalStockData;
import stocks.scrappers.HistoricalStockScraper;
import stocks.services.PredictionResponse;
import stocks.services.PredictorService;

class GameApiException extends RuntimeException {
    private final String errorType;
    private static final Logger logger = Logger.getLogger(GameApiException.class.getName());
    
    public GameApiException(String message, String errorType) {
        super(message);
        this.errorType = errorType;
        logger.log(Level.SEVERE, String.format("[%s] %s", errorType, message));
    }
    
    public String getErrorType() {
        return errorType;
    }
}

@GraphQLApi
public class GameApi {
    private static final Logger logger = Logger.getLogger(GameApi.class.getName());
    
    @Inject
    RedissonClient redissonClient;
    
    @Inject
    ObjectMapper objectMapper;
    
    @Inject
    PredictorService predictionService;

    private static final ZoneId NEW_YORK_ZONE = ZoneId.of("America/New_York");
    private static final LocalTime MARKET_OPEN = LocalTime.of(9, 30);
    private static final LocalTime MARKET_CLOSE = LocalTime.of(16, 0);
    private static final LocalTime RESULT_TIME = LocalTime.of(17, 0); // 5 PM

    private boolean isMarketOpen() {
        ZonedDateTime nyTime = ZonedDateTime.now(NEW_YORK_ZONE);
        LocalTime currentTime = nyTime.toLocalTime();
        DayOfWeek dayOfWeek = nyTime.getDayOfWeek();

        return dayOfWeek != DayOfWeek.SATURDAY && 
               dayOfWeek != DayOfWeek.SUNDAY && 
               currentTime.isAfter(MARKET_OPEN) && 
               currentTime.isBefore(MARKET_CLOSE);
    }

    private boolean canProcessResults() {
        ZonedDateTime nyTime = ZonedDateTime.now(NEW_YORK_ZONE);
        LocalTime currentTime = nyTime.toLocalTime();
        DayOfWeek dayOfWeek = nyTime.getDayOfWeek();

        // For regular weekdays
        if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
            return currentTime.isAfter(RESULT_TIME);
        }

        return false; // No result processing on weekends
    }

    private boolean isSundayGame(GameData game) {
        LocalDate gameDate = Instant.ofEpochMilli(Long.parseLong(game.getGameId()))
            .atZone(NEW_YORK_ZONE)
            .toLocalDate();
        return gameDate.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private LocalDate getEvaluationDate() {
        LocalDate today = LocalDate.now(NEW_YORK_ZONE);
        DayOfWeek dayOfWeek = today.getDayOfWeek();

        if (dayOfWeek == DayOfWeek.SUNDAY) {
            // For Sunday games, evaluation date is next Monday
            return today.plusDays(1);
        }
        
        // For regular weekday games
        if (!canProcessResults()) {
            today = today.minusDays(1);
        }
        
        // Skip weekends for regular games
        while (today.getDayOfWeek() == DayOfWeek.SATURDAY || 
               today.getDayOfWeek() == DayOfWeek.SUNDAY) {
            today = today.minusDays(1);
        }
        
        return today;
    }

    @Query("getGamesByUser")
    public List<GameData> getGamesByUser(@Name("email") String email) {
        try {
            RMapCache<String, String> cache = redissonClient.getMapCache("gamesCache");
            List<GameData> games = new ArrayList<>();
            
            cache.readAllValues().forEach(value -> {
                try {
                    GameData game = objectMapper.readValue(value, GameData.class);
                    if (game.getEmail().equals(email)) {
                        // Check if it's a Sunday game and results aren't ready
                        if (isSundayGame(game)) {
                            LocalDate gameDate = Instant.ofEpochMilli(Long.parseLong(game.getGameId()))
                                .atZone(NEW_YORK_ZONE)
                                .toLocalDate();
                            LocalDate nextMonday = gameDate.plusDays(1);
                            ZonedDateTime currentTime = ZonedDateTime.now(NEW_YORK_ZONE);
                            
                            // If it's not yet Monday 5 PM, show provisional result
                            if (currentTime.toLocalDate().isBefore(nextMonday) || 
                                (currentTime.toLocalDate().equals(nextMonday) && 
                                 currentTime.toLocalTime().isBefore(RESULT_TIME))) {
                                game = new GameData(
                                    game.getGameId(),
                                    game.getEmail(),
                                    game.getSymbol(),
                                    game.getUserPrediction(),
                                    false,  // provisional result
                                    0      // provisional coins
                                );
                            }
                        }
                        games.add(game);
                    }
                } catch (Exception e) {
                    String errorMsg = String.format("Error parsing game data for email %s", email);
                    logger.log(Level.SEVERE, errorMsg);
                    throw new GameApiException(errorMsg, "PARSE_ERROR");
                }
            });
            return games;
        } catch (GameApiException e) {
            throw e;
        } catch (Exception e) {
            String errorMsg = String.format("Error fetching games for email %s", email);
            logger.log(Level.SEVERE, errorMsg);
            throw new GameApiException(errorMsg, "FETCH_ERROR");
        }
    }

    @Mutation("recordGame")
public GameData recordGame(
        @Name("email") String email,
        @Name("symbol") String symbol,
        @Name("userPrediction") String userPrediction) {
    try {
        ZonedDateTime nyTime = ZonedDateTime.now(NEW_YORK_ZONE);
        String timeStr = nyTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        DayOfWeek dayOfWeek = nyTime.getDayOfWeek();

        // Always allow predictions to be recorded
        GameData game = new GameData(
                String.valueOf(System.currentTimeMillis()),
                email,
                symbol,
                userPrediction,
                false,  // provisional result
                0       // provisional coins
        );

        // Evaluate the result only outside market hours
        if (!isMarketOpen() && canProcessResults()) {
            LocalDate evaluationDate = getEvaluationDate();
            LocalDate startDate = evaluationDate.minusYears(2);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            PredictionResponse prediction = predictionService.predictStockPrice(
                symbol, 
                startDate.format(formatter), 
                evaluationDate.format(formatter)
            );

            List<HistoricalStockData> actualPrices = HistoricalStockScraper.fetchHistoricalData(
                symbol, evaluationDate, evaluationDate);

            if (actualPrices.isEmpty()) {
                throw new GameApiException(
                    String.format("No market data available for %s on %s", symbol, evaluationDate),
                    "NO_DATA"
                );
            }

            HistoricalStockData actualPrice = actualPrices.get(0);
            double actualPriceValue = Double.parseDouble(actualPrice.getClose());
            double predictedPrice = prediction.getPredictedClosingPrice();
            double userPredictedPrice = Double.parseDouble(userPrediction);

            double threshold = predictedPrice * 0.005;
            if (Math.abs(userPredictedPrice - actualPriceValue) <= threshold && 
                Math.abs(predictedPrice - actualPriceValue) <= threshold) {
                game = new GameData(game.getGameId(), email, symbol, userPrediction, true, 100);
            }
            else if (Math.abs(userPredictedPrice - actualPriceValue) <= threshold && 
                     Math.abs(predictedPrice - actualPriceValue) > threshold) {
                game = new GameData(game.getGameId(), email, symbol, userPrediction, true, 200);
            }
            else {
                game = new GameData(game.getGameId(), email, symbol, userPrediction, false, -10);
            }
        }

        RMapCache<String, String> cache = redissonClient.getMapCache("gamesCache");
        cache.put(game.getGameId(), objectMapper.writeValueAsString(game), 7, TimeUnit.DAYS);

        logger.log(Level.INFO, String.format("Successfully recorded game for email %s, symbol %s", email, symbol));

        return game;
    } catch (GameApiException e) {
        throw e;
    } catch (Exception e) {
        String errorMsg = String.format("Error processing game: %s", e.getMessage());
        logger.log(Level.SEVERE, errorMsg);
        throw new GameApiException(errorMsg, "PROCESSING_ERROR");
    }
}

}