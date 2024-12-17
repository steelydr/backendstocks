package stocks.scrappers;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import stocks.models.TickerData;
import stocks.models.TickerData.Ask;
import stocks.models.TickerData.Bid;
import stocks.models.TickerData.DateRange;
import stocks.models.TickerData.Range;

@jakarta.enterprise.context.ApplicationScoped
public class TickerDetailsScraper {

    private static final NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);
    private static final int MAX_RETRIES = 3;
    private static final int INITIAL_TIMEOUT = 30000; // 30 seconds

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isEmpty() || value.equals("--")) return null;
        try {
            return new BigDecimal(value.replaceAll("[^\\d.-]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private long parseLong(String value) {
        if (value == null || value.isEmpty() || value.equals("--")) return 0L;
        try {
            return numberFormat.parse(value.replaceAll("[^\\d,]", "")).longValue();
        } catch (ParseException e) {
            return 0L;
        }
    }

    private Double parseDouble(String value) {
        if (value == null || value.isEmpty() || value.equals("--")) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value.replaceAll("[^\\d.-]", ""));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private Range parseRange(String value) {
        if (value == null || value.isEmpty() || value.equals("--")) return null;
        String[] parts = value.split(" - ");
        if (parts.length != 2) return null;
        return new Range(
            parseBigDecimal(parts[0].trim()),
            parseBigDecimal(parts[1].trim())
        );
    }

    private DateRange parseDateRange(String value) {
        if (value == null || value.isEmpty() || value.equals("--")) return null;
        String[] parts = value.split(" - ");
        if (parts.length != 2) return null;
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US);
        try {
            LocalDate startDate = LocalDate.parse(parts[0].trim(), formatter);
            LocalDate endDate = LocalDate.parse(parts[1].trim(), formatter);
            return new DateRange(startDate, endDate);
        } catch (Exception e) {
            return null;
        }
    }

    public TickerData fetchTickerDetails(String symbol) {
        TickerData tickerData = new TickerData();
        int retries = 0;
        
        while (retries < MAX_RETRIES) {
            try {
                String baseUrl = String.format("https://finance.yahoo.com/quote/%s", symbol);
                System.out.println("Connecting to URL: " + baseUrl + " (Attempt " + (retries + 1) + ")");
                
                Document doc = Jsoup.connect(baseUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                        .header("Accept-Language", "en-US,en;q=0.5")
                        .header("Accept-Encoding", "gzip, deflate, br")
                        .header("DNT", "1")
                        .header("Connection", "keep-alive")
                        .header("Upgrade-Insecure-Requests", "1")
                        .header("Sec-Fetch-Dest", "document")
                        .header("Sec-Fetch-Mode", "navigate")
                        .header("Sec-Fetch-Site", "none")
                        .header("Sec-Fetch-User", "?1")
                        .header("Cache-Control", "max-age=0")
                        .timeout(INITIAL_TIMEOUT * (retries + 1))
                        .get();
                
                System.out.println("Connected successfully. Parsing data...");
                
                // Update: Use the new summary container selector
                Elements summaryContainers = doc.select("#nimbus-app > section > section > section > article > div.container.yf-dudngy");
                
                if (summaryContainers.isEmpty()) {
                    throw new IOException("Invalid response - summary container not found with the new selector");
                }

                for (Element ul : summaryContainers) {
                    Elements rows = ul.select("li"); // Assuming each data point is within a <li> element
                    for (Element row : rows) {
                        Element labelElement = row.selectFirst("span:first-child");
                        Element valueElement = row.selectFirst("span:last-child");
                        
                        if (labelElement != null && valueElement != null) {
                            String label = labelElement.text().trim();
                            String value = valueElement.text().trim();
                            
                            System.out.println("Processing: " + label + " = " + value);
                            processDataPoint(tickerData, label, value);
                        }
                    }
                }
                
                // If we got here, we successfully parsed the data
                return tickerData;
                
            } catch (IOException e) {
                System.err.println("Error fetching data (Attempt " + (retries + 1) + "): " + e.getMessage());
                retries++;
                
                if (retries < MAX_RETRIES) {
                    try {
                        // Exponential backoff
                        Thread.sleep(1000L * (long) Math.pow(2, retries));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        // If we get here, all retries failed
        System.err.println("Failed to fetch data after " + MAX_RETRIES + " attempts");
        return tickerData;
    }

    private void processDataPoint(TickerData tickerData, String label, String value) {
        switch (label) {
            case "Previous Close":
                tickerData.setPreviousClose(parseBigDecimal(value));
                break;
            case "Open":
                tickerData.setOpenPrice(parseBigDecimal(value));
                break;
            case "Bid":
                String[] bidParts = value.split(" x ");
                if (bidParts.length == 2) {
                    tickerData.setBid(new Bid(
                        parseBigDecimal(bidParts[0]),
                        Integer.parseInt(bidParts[1].replaceAll("[^\\d]", ""))
                    ));
                }
                break;
            case "Ask":
                String[] askParts = value.split(" x ");
                if (askParts.length == 2) {
                    tickerData.setAsk(new Ask(
                        parseBigDecimal(askParts[0]),
                        Integer.parseInt(askParts[1].replaceAll("[^\\d]", ""))
                    ));
                }
                break;
            case "Day's Range":
                tickerData.setDaysRange(parseRange(value));
                break;
            case "52 Week Range":
                tickerData.setWeekRange(parseRange(value));
                break;
            case "Volume":
                tickerData.setVolume(parseLong(value));
                break;
            case "Avg. Volume":
                tickerData.setAvgVolume(parseLong(value));
                break;
            case "Market Cap":
                tickerData.setMarketCap(value);
                break;
            case "Beta (5Y Monthly)":
                tickerData.setBeta(parseDouble(value));
                break;
            case "PE Ratio (TTM)":
                tickerData.setPeRatio(parseDouble(value));
                break;
            case "EPS (TTM)":
                tickerData.setEps(parseDouble(value));
                break;
            case "Earnings Date":
                tickerData.setEarningsDate(parseDateRange(value));
                break;
            case "Forward Dividend & Yield":
                tickerData.setDividendYield(value);
                break;
            case "Ex-Dividend Date":
                tickerData.setExDividendDate(value);
                break;
            case "1y Target Est":
                tickerData.setTargetEstimate(parseBigDecimal(value));
                break;
            default:
                System.out.println("Unrecognized label: " + label);
                break;
        }
    }
}
