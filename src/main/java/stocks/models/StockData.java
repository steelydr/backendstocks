package stocks.models;

import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;

public class StockData {

    @JsonbProperty("symbol")
    private String symbol;

    @JsonbProperty("name")
    private String name;

    @JsonbProperty("price")
    private String price;

    @JsonbProperty("change")
    private String change;

    @JsonbProperty("changePercent")
    private String changePercent;

    @JsonbProperty("volume")
    private String volume;

    @JsonbProperty("avgVolume")
    private String avgVolume;

    @JsonbProperty("marketCap")
    private String marketCap;

    @JsonbProperty("peRatio")
    private String peRatio;

    // Default no-args constructor required for JSON-B
    public StockData() {
    }

    @JsonbCreator
    public StockData(
        @JsonbProperty("symbol") String symbol,
        @JsonbProperty("name") String name,
        @JsonbProperty("price") String price,
        @JsonbProperty("change") String change,
        @JsonbProperty("changePercent") String changePercent,
        @JsonbProperty("volume") String volume,
        @JsonbProperty("avgVolume") String avgVolume,
        @JsonbProperty("marketCap") String marketCap,
        @JsonbProperty("peRatio") String peRatio
    ) {
        this.symbol = symbol;
        this.name = name;
        this.price = price;
        this.change = change;
        this.changePercent = changePercent;
        this.volume = volume;
        this.avgVolume = avgVolume;
        this.marketCap = marketCap;
        this.peRatio = peRatio;
    }

    // Getters and setters
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public String getChange() { return change; }
    public void setChange(String change) { this.change = change; }

    public String getChangePercent() { return changePercent; }
    public void setChangePercent(String changePercent) { this.changePercent = changePercent; }

    public String getVolume() { return volume; }
    public void setVolume(String volume) { this.volume = volume; }

    public String getAvgVolume() { return avgVolume; }
    public void setAvgVolume(String avgVolume) { this.avgVolume = avgVolume; }

    public String getMarketCap() { return marketCap; }
    public void setMarketCap(String marketCap) { this.marketCap = marketCap; }

    public String getPeRatio() { return peRatio; }
    public void setPeRatio(String peRatio) { this.peRatio = peRatio; }

    @Override
    public String toString() {
        return "StockData{" +
                "symbol='" + symbol + '\'' +
                ", name='" + name + '\'' +
                ", price='" + price + '\'' +
                ", change='" + change + '\'' +
                ", changePercent='" + changePercent + '\'' +
                ", volume='" + volume + '\'' +
                ", avgVolume='" + avgVolume + '\'' +
                ", marketCap='" + marketCap + '\'' +
                ", peRatio='" + peRatio + '\'' +
                '}';
    }
}
