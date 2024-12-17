package stocks.scrappers;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import stocks.models.StockData;

public class StockScraper {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static ScheduledFuture<?> currentTask;
    
    // Define market hours (assuming EST/EDT timezone)
    private static final LocalTime MARKET_OPEN = LocalTime.of(9, 30);
    private static final LocalTime MARKET_CLOSE = LocalTime.of(16, 0);
    
    // Define intervals
    private static final long DAY_INTERVAL_MS = 1; // 1 millisecond during market hours
    private static final long NIGHT_INTERVAL_MIN = 30; // 30 minutes during off-hours
    
    public static void startScheduledScraping(String url, String filterSymbol) {
        // Initial scheduling
        scheduleNextRun(url, filterSymbol);
        
        // Schedule a task to check and update intervals every minute
        scheduler.scheduleAtFixedRate(() -> {
            if (currentTask != null && !currentTask.isCancelled()) {
                currentTask.cancel(false);
            }
            scheduleNextRun(url, filterSymbol);
        }, 1, 1, TimeUnit.MILLISECONDS);
    }
    
    private static void scheduleNextRun(String url, String filterSymbol) {
        LocalDateTime now = LocalDateTime.now();
        LocalTime currentTime = now.toLocalTime();
        
        boolean isMarketHours = isWithinMarketHours(currentTime);
        long interval = isMarketHours ? DAY_INTERVAL_MS : TimeUnit.MINUTES.toMillis(NIGHT_INTERVAL_MIN);
        
        currentTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                List<StockData> stocks = fetchStocks(url, filterSymbol);
                System.out.println("Fetched " + stocks.size() + " stocks at: " + LocalDateTime.now() +
                                 " (Interval: " + (isMarketHours ? "1ms" : "30min") + ")");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, interval, TimeUnit.MILLISECONDS);
    }
    
    private static boolean isWithinMarketHours(LocalTime currentTime) {
        return !currentTime.isBefore(MARKET_OPEN) && !currentTime.isAfter(MARKET_CLOSE);
    }
    
    public static void stopScheduledScraping() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static List<StockData> fetchStocks(String url, String filterSymbol) {
        List<StockData> stocks = new ArrayList<>();
        
        try {
            Document doc = Jsoup.connect(url)
                            .userAgent("Mozilla/5.0")
                            .timeout(5000) // Added timeout
                            .get();
            
            Elements stockRows = doc.select("table tbody tr");
            
            for (Element row : stockRows) {
                if (stocks.size() >= 50) break;
                
                try {
                    String symbol = row.select("td:nth-child(1)").text();
                    
                    if (filterSymbol != null && !filterSymbol.isEmpty() && !symbol.equals(filterSymbol)) {
                        continue;
                    }
                    
                    String name = row.select("td:nth-child(2)").text();
                    String price = row.select("td:nth-child(4) fin-streamer[data-test=change]").attr("data-value");
                    String change = row.select("fin-streamer[data-test=colorChange]").attr("data-value");
                    String changePercent = row.select("td:nth-child(4) > span > div > fin-streamer:nth-child(2)").text()
                            .replaceAll("[()]", "");
                    String volume = row.select("td:nth-child(7)").text();
                    String avgVolume = row.select("td:nth-child(8)").text();
                    String marketCap = row.select("td:nth-child(9)").text();
                    String peRatio = row.select("td:nth-child(10)").text();
                    
                    if (!symbol.isEmpty() && !name.isEmpty()) {
                        stocks.add(new StockData(
                            symbol, name, price, change, changePercent,
                            volume, avgVolume, marketCap, peRatio
                        ));
                    }
                    
                    if (filterSymbol != null && !filterSymbol.isEmpty() && symbol.equals(filterSymbol)) {
                        break;
                    }
                } catch (Exception e) {
                    continue;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return stocks;
    }
}