package stocks;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import io.quarkus.qute.Template;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/stocks")
public class Stocks {

    @Inject
    Template homepagestocks;

    private static final Map<String, String> CATEGORY_URLS = Map.of(
        "most-active", "https://finance.yahoo.com/markets/stocks/most-active/",
        "trending", "https://finance.yahoo.com/markets/stocks/trending/",
        "gainers", "https://finance.yahoo.com/markets/stocks/gainers/",
        "losers", "https://finance.yahoo.com/markets/stocks/losers/"
    );
    
    @GET
    @Produces(MediaType.TEXT_HTML)
    public String getStocks(
        @QueryParam("category") @DefaultValue("most-active") String category,
        @QueryParam("symbol") @DefaultValue("") String symbol
    ) {
        String url = CATEGORY_URLS.getOrDefault(category, CATEGORY_URLS.get("most-active"));
        Map<String, StockData> stocks = fetchStocks(url);
        
        // If no symbol is selected, use the first stock
        if (symbol.isEmpty() && !stocks.isEmpty()) {
            symbol = stocks.keySet().iterator().next();
        }
        
        return this.homepagestocks
                .data("selectedStock", symbol)
                .data("selectedCategory", category)
                .data("stocks", stocks)
                .render();
    }

    private Map<String, StockData> fetchStocks(String url) {
        Map<String, StockData> stocks = new LinkedHashMap<>();
        
        try {
            Document doc = Jsoup.connect(url)
                              .userAgent("Mozilla/5.0")
                              .get();
            
            Elements stockRows = doc.select("table tbody tr");
            
            for (Element row : stockRows) {
                if (stocks.size() >= 50) break; // Limit to top 50
                
                try {
                    String symbol = row.select("td:nth-child(1)").text();
                    String name = row.select("td:nth-child(2)").text();
                    String price = row.select("td:nth-child(4) fin-streamer[data-test=change]").attr("data-value");
                    String change = row.select("fin-streamer[data-test=colorChange]").attr("data-value");
                    String changePercent = row.select("td:nth-child(4) > span > div > fin-streamer:nth-child(2)").text();
                    changePercent = changePercent.replaceAll("[()]", ""); // Remove both '(' and ')'
                    String volume = row.select("td:nth-child(7)").text();
                    String avgVolume = row.select("td:nth-child(8)").text();
                    String marketCap = row.select("td:nth-child(9)").text();
                    String peRatio = row.select("td:nth-child(10)").text();
                    
                    if (!symbol.isEmpty() && !name.isEmpty()) {
                        StockData stockData = new StockData(
                            name, price, change, changePercent,
                            volume, avgVolume, marketCap, peRatio
                        );
                        stocks.put(symbol, stockData);
                    }
                } catch (Exception e) {
                    continue; // Skip problematic entries
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return stocks;
    }

    public static class StockData {
        public final String name;
        public final String price;
        public final String change;
        public final String changePercent;
        public final String volume;
        public final String avgVolume;
        public final String marketCap;
        public final String peRatio;

        public StockData(String name, String price, String change, String changePercent,
                        String volume, String avgVolume, String marketCap, String peRatio) {
            this.name = name;
            this.price = price;
            this.change = change;
            this.changePercent = changePercent;
            this.volume = volume;
            this.avgVolume = avgVolume;
            this.marketCap = marketCap;
            this.peRatio = peRatio;
        }
    }
}