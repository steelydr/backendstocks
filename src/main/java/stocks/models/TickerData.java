package stocks.models;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Type;

@Type("TickerData")
@Description("Represents detailed information about a stock ticker.")
public class TickerData {
    private BigDecimal previousClose;
    private BigDecimal openPrice;
    private Bid bid;
    private Ask ask;
    private Range daysRange;
    private Range weekRange;
    private long volume;
    private long avgVolume;
    private String marketCap;
    private double beta;
    private double peRatio;
    private double eps;
    private DateRange earningsDate;
    private String dividendYield;
    private String exDividendDate;
    private BigDecimal targetEstimate;

    // Nested classes for complex data types
    @Type("Bid")
    @Description("Represents the bid price and size.")
    public static class Bid {
        @NonNull
        private BigDecimal price;
        private int size;

        public Bid(BigDecimal price, int size) {
            this.price = price;
            this.size = size;
        }

        public BigDecimal getPrice() { return price; }
        public int getSize() { return size; }
    }

    @Type("Ask")
    @Description("Represents the ask price and size.")
    public static class Ask {
        @NonNull
        private BigDecimal price;
        private int size;

        public Ask(BigDecimal price, int size) {
            this.price = price;
            this.size = size;
        }

        public BigDecimal getPrice() { return price; }
        public int getSize() { return size; }
    }

    @Type("Range")
    @Description("Represents a range with low and high values.")
    public static class Range {
        @NonNull
        private BigDecimal low;
        @NonNull
        private BigDecimal high;

        public Range(BigDecimal low, BigDecimal high) {
            this.low = low;
            this.high = high;
        }

        public BigDecimal getLow() { return low; }
        public BigDecimal getHigh() { return high; }
    }

    @Type("DateRange")
    @Description("Represents a date range with start and end dates.")
    public static class DateRange {
        @NonNull
        private LocalDate startDate;
        @NonNull
        private LocalDate endDate;

        public DateRange(LocalDate startDate, LocalDate endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }

        public LocalDate getStartDate() { return startDate; }
        public LocalDate getEndDate() { return endDate; }
    }

    // Getters and setters
    public BigDecimal getPreviousClose() { return previousClose; }
    public void setPreviousClose(BigDecimal previousClose) { this.previousClose = previousClose; }

    public BigDecimal getOpenPrice() { return openPrice; }
    public void setOpenPrice(BigDecimal openPrice) { this.openPrice = openPrice; }

    public Bid getBid() { return bid; }
    public void setBid(Bid bid) { this.bid = bid; }

    public Ask getAsk() { return ask; }
    public void setAsk(Ask ask) { this.ask = ask; }

    public Range getDaysRange() { return daysRange; }
    public void setDaysRange(Range daysRange) { this.daysRange = daysRange; }

    public Range getWeekRange() { return weekRange; }
    public void setWeekRange(Range weekRange) { this.weekRange = weekRange; }

    public long getVolume() { return volume; }
    public void setVolume(long volume) { this.volume = volume; }

    public long getAvgVolume() { return avgVolume; }
    public void setAvgVolume(long avgVolume) { this.avgVolume = avgVolume; }

    public String getMarketCap() { return marketCap; }
    public void setMarketCap(String marketCap) { this.marketCap = marketCap; }

    public double getBeta() { return beta; }
    public void setBeta(double beta) { this.beta = beta; }

    public double getPeRatio() { return peRatio; }
    public void setPeRatio(double peRatio) { this.peRatio = peRatio; }

    public double getEps() { return eps; }
    public void setEps(double eps) { this.eps = eps; }

    public DateRange getEarningsDate() { return earningsDate; }
    public void setEarningsDate(DateRange earningsDate) { this.earningsDate = earningsDate; }

    public String getDividendYield() { return dividendYield; }
    public void setDividendYield(String dividendYield) { this.dividendYield = dividendYield; }

    public String getExDividendDate() { return exDividendDate; }
    public void setExDividendDate(String exDividendDate) { this.exDividendDate = exDividendDate; }

    public BigDecimal getTargetEstimate() { return targetEstimate; }
    public void setTargetEstimate(BigDecimal targetEstimate) { this.targetEstimate = targetEstimate; }

    @Override
public String toString() {
    return String.format("""
        {
            "previousClose": "%s",
            "openPrice": "%s",
            "bid": {
                "price": "%s",
                "size": %d
            },
            "ask": {
                "price": "%s",
                "size": %d
            },
            "daysRange": {
                "low": "%s",
                "high": "%s"
            },
            "weekRange": {
                "low": "%s",
                "high": "%s"
            },
            "volume": %d,
            "averageVolume": %d,
            "marketCap": "%s",
            "beta": %.2f,
            "peRatio": %.2f,
            "eps": %.2f,
            "earningsDate": {
                "startDate": "%s",
                "endDate": "%s"
            },
            "dividendYield": "%s",
            "exDividendDate": "%s",
            "targetEstimate": "%s"
        }""",
        previousClose,
        openPrice,
        bid != null ? bid.getPrice() : "--",
        bid != null ? bid.getSize() : 0,
        ask != null ? ask.getPrice() : "--",
        ask != null ? ask.getSize() : 0,
        daysRange != null ? daysRange.getLow() : "--",
        daysRange != null ? daysRange.getHigh() : "--",
        weekRange != null ? weekRange.getLow() : "--",
        weekRange != null ? weekRange.getHigh() : "--",
        volume,
        avgVolume,
        marketCap,
        beta,
        peRatio,
        eps,
        earningsDate != null ? earningsDate.getStartDate() : "--",
        earningsDate != null ? earningsDate.getEndDate() : "--",
        dividendYield,
        exDividendDate,
        targetEstimate
    );
}
}