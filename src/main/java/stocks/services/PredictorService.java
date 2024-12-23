package stocks.services;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import jakarta.enterprise.context.ApplicationScoped;
import weka.classifiers.functions.LinearRegression;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

@ApplicationScoped
public class PredictorService {
    private static final String GRAPHQL_ENDPOINT = "http://192.168.40.86:80/graphql";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    public PredictionResponse predictStockPrice(String symbol, String startDate, String endDate) throws Exception {
        FetchResult fetchResult = fetchHistoricalData(symbol, startDate, endDate);
        List<PredictorModel> historicalData = fetchResult.getHistoricalData();

        if (historicalData.isEmpty()) {
            throw new Exception("No data available for symbol: " + symbol);
        }

        // Sort the historical data in ascending order by date
        historicalData.sort(Comparator.comparing(p -> p.date));

        LinearRegression model = new LinearRegression();
        Instances dataset = prepareDataset(historicalData);
        model.buildClassifier(dataset);

        PredictorModel lastDay = historicalData.get(historicalData.size() - 1);
        DenseInstance predictionInstance = new DenseInstance(1.0, new double[]{
                lastDay.open, lastDay.high, lastDay.low, lastDay.volume, lastDay.close, 0
        });
        predictionInstance.setDataset(dataset);
        double predictedClose = model.classifyInstance(predictionInstance);

        double predictedChange = ((predictedClose - lastDay.close) / lastDay.close) * 100;

        // Format model details
        String modelDetails = formatModelDetails(model);

        // Build PredictionResponse
        PredictionResponse response = new PredictionResponse();
        response.setDataPoints(historicalData.size());
        response.setSymbol(symbol);
        response.setDateRangeStart(historicalData.get(0).date.format(DATE_FORMATTER));
        response.setDateRangeEnd(lastDay.date.format(DATE_FORMATTER));
        response.setLastKnownDate(lastDay.date.format(DATE_FORMATTER));
        response.setLastClosingPrice(lastDay.close);
        response.setPredictedClosingPrice(predictedClose);
        response.setPredictedChange(predictedChange);
        response.setModelDetails(modelDetails);

        return response;
    }

    private FetchResult fetchHistoricalData(String symbol, String startDate, String endDate) throws Exception {
        String query = String.format("""
                {
                  historicalData(symbol: "%s", startDate: "%s", endDate: "%s") {
                    date
                    open
                    close
                    high
                    low
                    volume
                  }
                }
                """, symbol, startDate, endDate);

        String jsonRequest = "{ \"query\": \"" + query.replace("\"", "\\\"") + "\" }";
        Connection.Response response = Jsoup.connect(GRAPHQL_ENDPOINT)
                .header("Content-Type", "application/json")
                .requestBody(jsonRequest)
                .method(Connection.Method.POST)
                .ignoreContentType(true)
                .execute();

        String rawResponse = response.body();
        List<PredictorModel> historicalData = PredictorModel.parseResponse(rawResponse);

        return new FetchResult(rawResponse, historicalData);
    }

    private Instances prepareDataset(List<PredictorModel> historicalData) {
        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("open"));
        attributes.add(new Attribute("high"));
        attributes.add(new Attribute("low"));
        attributes.add(new Attribute("volume"));
        attributes.add(new Attribute("prev_close"));
        attributes.add(new Attribute("close"));

        Instances dataset = new Instances("StockData", attributes, historicalData.size());
        dataset.setClassIndex(5);

        for (int i = 1; i < historicalData.size(); i++) {
            PredictorModel current = historicalData.get(i);
            PredictorModel previous = historicalData.get(i - 1);
            dataset.add(new DenseInstance(1.0, new double[]{
                    current.open, current.high, current.low, current.volume, previous.close, current.close
            }));
        }
        return dataset;
    }

    private String formatModelDetails(LinearRegression model) {
        StringBuilder details = new StringBuilder();
        String[] lines = model.toString().split("\n");
        boolean coefficientsSection = false;
        for (String line : lines) {
            if (line.startsWith("close =")) {
                details.append(line.trim()).append("\n");
                coefficientsSection = true;
                continue;
            }
            if (coefficientsSection) {
                if (line.trim().isEmpty()) {
                    break;
                }
                details.append("     ").append(line.trim()).append("\n");
            }
        }
        return details.toString();
    }

    // Inner class to hold fetch results
    private static class FetchResult {
        private final String rawResponse;
        private final List<PredictorModel> historicalData;

        public FetchResult(String rawResponse, List<PredictorModel> historicalData) {
            this.rawResponse = rawResponse;
            this.historicalData = historicalData;
        }

        public String getRawResponse() {
            return rawResponse;
        }

        public List<PredictorModel> getHistoricalData() {
            return historicalData;
        }
    }
}
