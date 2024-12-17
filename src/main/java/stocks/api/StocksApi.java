package stocks.api;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.redisson.Redisson;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import stocks.models.StockData;
import stocks.scrappers.StockScraper;

@Singleton
@Path("/api/stocks")
public class StocksApi {
    private static final String BASE_URL = "https://finance.yahoo.com/markets/stocks/";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;
    private ScheduledExecutorService scheduler;
    
    public StocksApi() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://localhost:6379")
                .setConnectionPoolSize(10)
                .setConnectionMinimumIdleSize(2);
        this.redissonClient = Redisson.create(config);
        this.objectMapper = new ObjectMapper();
    }
    
    @PostConstruct
    public void init() {
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::updateAllStocks, 0, 1, TimeUnit.MINUTES);
        String timestamp = LocalDateTime.now().format(formatter);
        System.out.println(String.format("[%s] Stock update scheduler initialized", timestamp));
    }
    
    @PreDestroy
    public void cleanup() {
        try {
            scheduler.shutdown();
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            redissonClient.shutdown();
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    private void updateAllStocks() {
        try {
            updateStockCategory("most-active", BASE_URL + "most-active/", null);
            updateStockCategory("trending", BASE_URL + "trending/", null);
            updateStockCategory("gainers", BASE_URL + "gainers/", null);
            updateStockCategory("losers", BASE_URL + "losers/", null);
        } catch (Exception e) {
            String timestamp = LocalDateTime.now().format(formatter);
            System.err.println(String.format("[%s] Error updating stocks: %s", 
                timestamp, e.getMessage()));
        }
    }
    
    private void updateStockCategory(String category, String url, String filterSymbol) {
        try {
            getCachedResponse(category, url, filterSymbol);
        } catch (Exception e) {
            String timestamp = LocalDateTime.now().format(formatter);
            System.err.println(String.format("[%s] Error updating %s: %s", 
                timestamp, category, e.getMessage()));
        }
    }
    
    @GET
    @Path("/most-active")
    @Produces(MediaType.APPLICATION_JSON)
    public List<StockData> getMostActive(@QueryParam("symbol") String symbol) {
        return getCachedResponse("most-active", BASE_URL + "most-active/", symbol);
    }
    
    @GET
    @Path("/trending")
    @Produces(MediaType.APPLICATION_JSON)
    public List<StockData> getTrending(@QueryParam("symbol") String symbol) {
        return getCachedResponse("trending", BASE_URL + "trending/", symbol);
    }
    
    @GET
    @Path("/gainers")
    @Produces(MediaType.APPLICATION_JSON)
    public List<StockData> getGainers(@QueryParam("symbol") String symbol) {
        return getCachedResponse("gainers", BASE_URL + "gainers/", symbol);
    }
    
    @GET
    @Path("/losers")
    @Produces(MediaType.APPLICATION_JSON)
    public List<StockData> getLosers(@QueryParam("symbol") String symbol) {
        return getCachedResponse("losers", BASE_URL + "losers/", symbol);
    }
    
    private void logUpdate(String symbol, String field, String oldValue, String newValue) {
        String timestamp = LocalDateTime.now().format(formatter);
        System.out.println(String.format("[%s] Updating %s - %s: %s -> %s", 
            timestamp, symbol, field, oldValue, newValue));
    }
    
    private List<StockData> getCachedResponse(String key, String url, String filterSymbol) {
        try {
            RMapCache<String, String> cache = redissonClient.getMapCache("stocksCache");
            String cacheKey = key + (filterSymbol != null ? ":" + filterSymbol : "");
            
            List<StockData> freshData = StockScraper.fetchStocks(url, filterSymbol);
            String cachedData = cache.get(cacheKey);
            
            if (cachedData != null) {
                List<StockData> cachedStocks = objectMapper.readValue(
                    cachedData, 
                    new TypeReference<List<StockData>>() {}
                );
                
                boolean hasUpdates = false;
                
                Map<String, StockData> cachedStocksMap = cachedStocks.stream()
                    .collect(Collectors.toMap(StockData::getSymbol, stock -> stock));
                    
                for (StockData freshStock : freshData) {
                    String symbol = freshStock.getSymbol();
                    StockData cachedStock = cachedStocksMap.get(symbol);
                    
                    if (cachedStock != null) {
                        if (!cachedStock.getPrice().equals(freshStock.getPrice())) {
                            logUpdate(symbol, "price", cachedStock.getPrice(), freshStock.getPrice());
                            cachedStock.setPrice(freshStock.getPrice());
                            hasUpdates = true;
                        }
                        if (!cachedStock.getName().equals(freshStock.getName())) {
                            logUpdate(symbol, "name", cachedStock.getName(), freshStock.getName());
                            cachedStock.setName(freshStock.getName());
                            hasUpdates = true;
                        }
                        if (!cachedStock.getChange().equals(freshStock.getChange())) {
                            logUpdate(symbol, "change", cachedStock.getChange(), freshStock.getChange());
                            cachedStock.setChange(freshStock.getChange());
                            hasUpdates = true;
                        }
                        if (!cachedStock.getChangePercent().equals(freshStock.getChangePercent())) {
                            logUpdate(symbol, "changePercent", cachedStock.getChangePercent(), freshStock.getChangePercent());
                            cachedStock.setChangePercent(freshStock.getChangePercent());
                            hasUpdates = true;
                        }
                        if (!cachedStock.getVolume().equals(freshStock.getVolume())) {
                            logUpdate(symbol, "volume", cachedStock.getVolume(), freshStock.getVolume());
                            cachedStock.setVolume(freshStock.getVolume());
                            hasUpdates = true;
                        }
                        if (!cachedStock.getAvgVolume().equals(freshStock.getAvgVolume())) {
                            logUpdate(symbol, "avgVolume", cachedStock.getAvgVolume(), freshStock.getAvgVolume());
                            cachedStock.setAvgVolume(freshStock.getAvgVolume());
                            hasUpdates = true;
                        }
                        if (!cachedStock.getMarketCap().equals(freshStock.getMarketCap())) {
                            logUpdate(symbol, "marketCap", cachedStock.getMarketCap(), freshStock.getMarketCap());
                            cachedStock.setMarketCap(freshStock.getMarketCap());
                            hasUpdates = true;
                        }
                        if (!cachedStock.getPeRatio().equals(freshStock.getPeRatio())) {
                            logUpdate(symbol, "peRatio", cachedStock.getPeRatio(), freshStock.getPeRatio());
                            cachedStock.setPeRatio(freshStock.getPeRatio());
                            hasUpdates = true;
                        }
                    } else {
                        String timestamp = LocalDateTime.now().format(formatter);
                        System.out.println(String.format("[%s] Adding new stock: %s", 
                            timestamp, symbol));
                        cachedStocks.add(freshStock);
                        hasUpdates = true;
                    }
                }
                
                if (hasUpdates) {
                    cache.put(cacheKey, objectMapper.writeValueAsString(cachedStocks), 
                             20, TimeUnit.MILLISECONDS);
                }
                
                return cachedStocks;
            }
            
            String timestamp = LocalDateTime.now().format(formatter);
            System.out.println(String.format("[%s] Initial cache population for key: %s", 
                timestamp, cacheKey));
            cache.put(cacheKey, objectMapper.writeValueAsString(freshData), 
                     24, TimeUnit.HOURS);
            
            return freshData;
            
        } catch (Exception e) {
            throw new RuntimeException("Error fetching data or interacting with Redis", e);
        }
    }
}