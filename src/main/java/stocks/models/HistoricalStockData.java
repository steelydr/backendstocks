package stocks.models;

import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;

public class HistoricalStockData {
    @JsonbProperty("date")
    private String date;
    
    @JsonbProperty("open")
    private String open;
    
    @JsonbProperty("high")
    private String high;
    
    @JsonbProperty("low")
    private String low;
    
    @JsonbProperty("close")
    private String close;
    
    @JsonbProperty("adjClose")
    private String adjClose;
    
    @JsonbProperty("volume")
    private String volume;

    // Default no-args constructor required for JSON-B
    public HistoricalStockData() {
    }

    @JsonbCreator
    public HistoricalStockData(
        @JsonbProperty("date") String date,
        @JsonbProperty("open") String open,
        @JsonbProperty("high") String high,
        @JsonbProperty("low") String low,
        @JsonbProperty("close") String close,
        @JsonbProperty("adjClose") String adjClose,
        @JsonbProperty("volume") String volume
    ) {
        this.date = date;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.adjClose = adjClose;
        this.volume = volume;
    }

    // Getters and setters
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getOpen() { return open; }
    public void setOpen(String open) { this.open = open; }

    public String getHigh() { return high; }
    public void setHigh(String high) { this.high = high; }

    public String getLow() { return low; }
    public void setLow(String low) { this.low = low; }

    public String getClose() { return close; }
    public void setClose(String close) { this.close = close; }

    public String getAdjClose() { return adjClose; }
    public void setAdjClose(String adjClose) { this.adjClose = adjClose; }

    public String getVolume() { return volume; }
    public void setVolume(String volume) { this.volume = volume; }

    @Override
    public String toString() {
        return "HistoricalStockData{" +
                "date='" + date + '\'' +
                ", open='" + open + '\'' +
                ", high='" + high + '\'' +
                ", low='" + low + '\'' +
                ", close='" + close + '\'' +
                ", adjClose='" + adjClose + '\'' +
                ", volume='" + volume + '\'' +
                '}';
    }
}
