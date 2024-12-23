package stocks.api;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;
import org.redisson.Redisson;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import com.fasterxml.jackson.databind.ObjectMapper;

import stocks.models.HistoricalStockData;
import stocks.scrappers.HistoricalStockScraper;

@GraphQLApi
public class HistoricalStocksApi {

    private static final RedissonClient redissonClient;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://localhost:6379")
                .setConnectionPoolSize(10)
                .setConnectionMinimumIdleSize(2);
        redissonClient = Redisson.create(config);
    }

    @Query("historicalData")
    public List<HistoricalStockData> getHistoricalData(
        @Name("symbol") String symbol,
        @Name("startDate") String startDate,
        @Name("endDate") String endDate
    ) {
        if (symbol == null || startDate == null || endDate == null) {
            throw new IllegalArgumentException("Symbol, start date, and end date are required");
        }

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        String cacheKey = generateCacheKey(symbol, startDate, endDate);
        return getCachedResponse(cacheKey, symbol, start, end);
    }

    private List<HistoricalStockData> getCachedResponse(String cacheKey, String symbol, LocalDate start, LocalDate end) {
        try {
            RMapCache<String, String> cache = redissonClient.getMapCache("historicalStocksCache");

            String cachedData = cache.get(cacheKey);

            if (cachedData != null) {
                System.out.println("Using data from Redis cache for key: " + cacheKey);
                return objectMapper.readValue(
                    cachedData, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, HistoricalStockData.class)
                );
            }

            System.out.println("Fetching fresh data from source for key: " + cacheKey);
            List<HistoricalStockData> freshData = HistoricalStockScraper.fetchHistoricalData(symbol, start, end);

            // Cache the result for 4 hours
            cache.put(cacheKey, objectMapper.writeValueAsString(freshData), 4, TimeUnit.HOURS);

            return freshData;
        } catch (Exception e) {
            throw new RuntimeException("Error fetching data or interacting with Redis", e);
        }
    }

    private String generateCacheKey(String symbol, String startDate, String endDate) {
        return String.format("%s:%s:%s", symbol, startDate, endDate);
    }
}
