package stocks.scrappers;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import stocks.models.HistoricalStockData;

public class HistoricalStockScraper {
    
    public static List<HistoricalStockData> fetchHistoricalData(String symbol, LocalDate startDate, LocalDate endDate) {
        List<HistoricalStockData> historicalData = new ArrayList<>();
        
        try {
            // Convert dates to Unix timestamp (seconds since epoch)
            long period1 = startDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
            long period2 = endDate.atTime(23, 59, 59).toEpochSecond(ZoneOffset.UTC);
            
            String url = String.format(
                "https://finance.yahoo.com/quote/%s/history?period1=%d&period2=%d&interval=1d",
                symbol, period1, period2
            );
            
            Document doc = Jsoup.connect(url)
                              .userAgent("Mozilla/5.0")
                              .get();
            
            Elements rows = doc.select("table tbody tr");
            
            for (Element row : rows) {
                try {
                    Elements cells = row.select("td");
                    if (cells.size() >= 7) {
                        String date = cells.get(0).text();
                        String open = cells.get(1).text();
                        String high = cells.get(2).text();
                        String low = cells.get(3).text();
                        String close = cells.get(4).text();
                        String adjClose = cells.get(5).text();
                        String volume = cells.get(6).text();
                        
                        if (!date.isEmpty() && !open.equals("Dividend")) {  // Skip dividend rows
                            historicalData.add(new HistoricalStockData(
                                date, open, high, low, close, adjClose, volume
                            ));
                        }
                    }
                } catch (Exception e) {
                    continue;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return historicalData;
    }
}
