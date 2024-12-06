package stocks.api;

import java.util.Map;
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
import stocks.models.StockData;
import stocks.scrappers.StockScraper;

@Path("/api/stocks")
public class StocksApi {

    private static final String BASE_URL = "https://finance.yahoo.com/markets/stocks/";

    // Initialize Redisson client
    private static final RedissonClient redissonClient;
    private static final ObjectMapper objectMapper = new ObjectMapper(); // Jackson ObjectMapper for JSON handling

    static {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://localhost:6379") // Update with your Redis host/port
                .setConnectionPoolSize(10)
                .setConnectionMinimumIdleSize(2);
        redissonClient = Redisson.create(config);
    }

    @GET
    @Path("/most-active")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, StockData> getMostActive(@QueryParam("symbol") String symbol) {
        return getCachedResponse("most-active", BASE_URL + "most-active/", symbol);
    }

    @GET
    @Path("/trending")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, StockData> getTrending(@QueryParam("symbol") String symbol) {
        return getCachedResponse("trending", BASE_URL + "trending/", symbol);
    }

    @GET
    @Path("/gainers")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, StockData> getGainers(@QueryParam("symbol") String symbol) {
        return getCachedResponse("gainers", BASE_URL + "gainers/", symbol);
    }

    @GET
    @Path("/losers")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, StockData> getLosers(@QueryParam("symbol") String symbol) {
        return getCachedResponse("losers", BASE_URL + "losers/", symbol);
    }

    private Map<String, StockData> getCachedResponse(String key, String url, String filterSymbol) {
        try {
            // Redisson map cache for caching data
            RMapCache<String, String> cache = redissonClient.getMapCache("stocksCache");

            String cacheKey = key + (filterSymbol != null ? ":" + filterSymbol : "");
            String cachedData = cache.get(cacheKey);

            if (cachedData != null) {
                System.out.println("Using data from Redis cache for key: " + cacheKey);
                // Deserialize JSON to Map
                return objectMapper.readValue(cachedData, Map.class);
            }

            System.out.println("Fetching fresh data from source for key: " + cacheKey);
            // Fetch fresh data from the source
            Map<String, StockData> freshData = StockScraper.fetchStocks(url, filterSymbol);

            // Serialize and cache the data with a 4-hour expiration
            cache.put(cacheKey, objectMapper.writeValueAsString(freshData), 4, TimeUnit.HOURS);

            return freshData;
        } catch (Exception e) {
            throw new RuntimeException("Error fetching data or interacting with Redis", e);
        }
    }
}
