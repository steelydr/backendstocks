package stocks.scrappers;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import stocks.models.StockData;

public class StockScraper {

    public static Map<String, StockData> fetchStocks(String url, String filterSymbol) {
        Map<String, StockData> stocks = new LinkedHashMap<>();
        
        try {
            Document doc = Jsoup.connect(url)
                                .userAgent("Mozilla/5.0")
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
                        stocks.put(symbol, new StockData(
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
