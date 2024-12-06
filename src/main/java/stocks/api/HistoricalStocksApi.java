package stocks.api;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.redisson.Redisson;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import stocks.models.HistoricalStockData;
import stocks.scrappers.HistoricalStockScraper;

@Path("/api/stocks/historical")
public class HistoricalStocksApi {

    // Initialize Redisson client
    private static final RedissonClient redissonClient;
    private static final ObjectMapper objectMapper = new ObjectMapper(); // For JSON handling

    static {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://localhost:6379") // Update with your Redis host/port
                .setConnectionPoolSize(10)
                .setConnectionMinimumIdleSize(2);
        redissonClient = Redisson.create(config);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<HistoricalStockData> getHistoricalData(
        @QueryParam("symbol") String symbol,
        @QueryParam("startDate") String startDate,
        @QueryParam("endDate") String endDate
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
            // Redisson map cache for caching data
            RMapCache<String, String> cache = redissonClient.getMapCache("historicalStocksCache");

            String cachedData = cache.get(cacheKey);

            if (cachedData != null) {
                System.out.println("Using data from Redis cache for key: " + cacheKey);
                // Deserialize JSON to List
                return objectMapper.readValue(cachedData, objectMapper.getTypeFactory().constructCollectionType(List.class, HistoricalStockData.class));
            }

            System.out.println("Fetching fresh data from source for key: " + cacheKey);
            // Fetch fresh data from the scraper
            List<HistoricalStockData> freshData = HistoricalStockScraper.fetchHistoricalData(symbol, start, end);

            // Serialize and cache the data with a 4-hour expiration
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
