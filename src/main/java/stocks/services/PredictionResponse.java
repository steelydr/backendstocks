package stocks.services;

public class PredictionResponse {
    private int dataPoints;
    private String symbol;
    private String dateRangeStart;
    private String dateRangeEnd;
    private String lastKnownDate;
    private double lastClosingPrice;
    private double predictedClosingPrice;
    private double predictedChange;
    private String modelDetails;
    private String error; // Optional: To handle errors gracefully

    // Getters and Setters

    public int getDataPoints() {
        return dataPoints;
    }

    public void setDataPoints(int dataPoints) {
        this.dataPoints = dataPoints;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getDateRangeStart() {
        return dateRangeStart;
    }

    public void setDateRangeStart(String dateRangeStart) {
        this.dateRangeStart = dateRangeStart;
    }

    public String getDateRangeEnd() {
        return dateRangeEnd;
    }

    public void setDateRangeEnd(String dateRangeEnd) {
        this.dateRangeEnd = dateRangeEnd;
    }

    public String getLastKnownDate() {
        return lastKnownDate;
    }

    public void setLastKnownDate(String lastKnownDate) {
        this.lastKnownDate = lastKnownDate;
    }

    public double getLastClosingPrice() {
        return lastClosingPrice;
    }

    public void setLastClosingPrice(double lastClosingPrice) {
        this.lastClosingPrice = lastClosingPrice;
    }

    public double getPredictedClosingPrice() {
        return predictedClosingPrice;
    }

    public void setPredictedClosingPrice(double predictedClosingPrice) {
        this.predictedClosingPrice = predictedClosingPrice;
    }

    public double getPredictedChange() {
        return predictedChange;
    }

    public void setPredictedChange(double predictedChange) {
        this.predictedChange = predictedChange;
    }

    public String getModelDetails() {
        return modelDetails;
    }

    public void setModelDetails(String modelDetails) {
        this.modelDetails = modelDetails;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
