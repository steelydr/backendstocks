package stocks.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.redisson.api.RedissonClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import weka.classifiers.functions.LinearRegression;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

@ApplicationScoped
public class MachineLearningService {

    @Inject
    RedissonClient redissonClient;

    @Inject
    ObjectMapper objectMapper;

    /**
     * Predicts the next day's stock price based on historical data.
     *
     * @param symbol The stock symbol.
     * @return The predicted next day's stock price.
     */
    public double predictNextDayPrice(String symbol) {
        try {
            // Updated date range for historical data
            List<StockData> historicalData = fetchHistoricalData(symbol, "2024-11-01", "2024-12-01");

            // Prepare dataset for Weka
            Instances dataset = prepareDataset(historicalData);

            // Train Linear Regression model
            LinearRegression model = new LinearRegression();
            model.buildClassifier(dataset);

            // Create a new instance for prediction (using the last available data)
            StockData lastDay = historicalData.get(historicalData.size() - 1);
            Instance instance = new DenseInstance(dataset.numAttributes());
            instance.setValue(dataset.attribute("open"), lastDay.getOpen());
            instance.setValue(dataset.attribute("high"), lastDay.getHigh());
            instance.setValue(dataset.attribute("low"), lastDay.getLow());
            instance.setValue(dataset.attribute("volume"), lastDay.getVolume());
            dataset.add(instance);
            instance.setDataset(dataset);

            // Predict the 'close' price for the next day (December 2, 2024)
            double predictedClose = model.classifyInstance(instance);
            return predictedClose;
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback to fetching the actual price if prediction fails
            return fetchActualPrice(symbol, "2024-12-02");
        }
    }

    /**
     * Evaluates whether the user's prediction is correct within a certain threshold.
     *
     * @param predictedPrice The price predicted by the user.
     * @param actualPrice    The actual price of the stock.
     * @return True if the prediction is within the acceptable range, else false.
     */
    public boolean evaluatePrediction(double predictedPrice, double actualPrice) {
        double threshold = 0.02; // 2% threshold for accuracy
        double difference = Math.abs(predictedPrice - actualPrice) / actualPrice;
        return difference <= threshold;
    }

    /**
     * Calculates the number of SuperCoins earned based on prediction accuracy.
     *
     * @param isCorrect       Whether the prediction was correct.
     * @param predictedPrice  The price predicted by the user.
     * @param actualPrice     The actual price of the stock.
     * @return The number of SuperCoins earned.
     */
    public int calculateSuperCoins(boolean isCorrect, double predictedPrice, double actualPrice) {
        if (isCorrect) {
            // SuperCoins based on the accuracy of the prediction
            double accuracy = 1 - (Math.abs(predictedPrice - actualPrice) / actualPrice);
            return (int) (50 + (accuracy * 50)); // Between 50 to 100 coins
        } else {
            // If prediction is incorrect but user is still correct in direction
            boolean correctDirection = (predictedPrice > actualPrice && actualPrice > 0) ||
                                       (predictedPrice < actualPrice && actualPrice > 0);
            if (correctDirection) {
                return 100;
            } else {
                return 10; // Minimal coins for participation
            }
        }
    }

    /**
     * Fetches historical stock data using a GraphQL query.
     *
     * @param symbol    The stock symbol.
     * @param startDate The start date in YYYY-MM-DD format.
     * @param endDate   The end date in YYYY-MM-DD format.
     * @return A list of StockData objects.
     * @throws Exception If an error occurs during data fetching or parsing.
     */
    private List<StockData> fetchHistoricalData(String symbol, String startDate, String endDate) throws Exception {
        String graphqlEndpoint = "http://192.168.40.86:80/graphql"; // Replace with your actual endpoint
        String query = "{\n" +
                "  historicalData(\n" +
                "    symbol: \"" + symbol + "\",\n" +
                "    startDate: \"" + startDate + "\",\n" +
                "    endDate: \"" + endDate + "\"\n" +
                "  ) {\n" +
                "    date\n" +
                "    open\n" +
                "    high\n" +
                "    low\n" +
                "    close\n" +
                "    adjClose\n" +
                "    volume\n" +
                "  }\n" +
                "}";

        URL url = new URL(graphqlEndpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String jsonInputString = "{\"query\": \"" + query.replace("\n", " ").replace("\"", "\\\"") + "\"}";

        try (java.io.OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int code = conn.getResponseCode();
        if (code != 200) {
            throw new RuntimeException("Failed to fetch historical data: HTTP error code : " + code);
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder responseStrBuilder = new StringBuilder();
        String responseLine;
        while ((responseLine = br.readLine()) != null) {
            responseStrBuilder.append(responseLine.trim());
        }

        JsonNode responseJson = objectMapper.readTree(responseStrBuilder.toString());
        ArrayNode dataArray = (ArrayNode) responseJson.path("data").path("historicalData");

        List<StockData> historicalData = new ArrayList<>();
        for (JsonNode node : dataArray) {
            StockData stock = new StockData(
                    node.get("date").asText(),
                    node.get("open").asDouble(),
                    node.get("high").asDouble(),
                    node.get("low").asDouble(),
                    node.get("close").asDouble(),
                    node.get("adjClose").asDouble(),
                    node.get("volume").asLong()
            );
            historicalData.add(stock);
        }

        return historicalData;
    }

    /**
     * Fetches the actual stock price for a specific date using a GraphQL query.
     *
     * @param symbol The stock symbol.
     * @param date   The date for which to fetch the actual price in YYYY-MM-DD format.
     * @return The actual stock price for the specified date.
     */
    private double fetchActualPrice(String symbol, String date) {
        try {
            String graphqlEndpoint = "http://192.168.40.86:80/graphql"; // Replace with your actual endpoint
            String query = "{\n" +
                    "  historicalData(\n" +
                    "    symbol: \"" + symbol + "\",\n" +
                    "    startDate: \"" + date + "\",\n" +
                    "    endDate: \"" + date + "\"\n" +
                    "  ) {\n" +
                    "    close\n" +
                    "  }\n" +
                    "}";

            URL url = new URL(graphqlEndpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String jsonInputString = "{\"query\": \"" + query.replace("\n", " ").replace("\"", "\\\"") + "\"}";

            try (java.io.OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int code = conn.getResponseCode();
            if (code != 200) {
                throw new RuntimeException("Failed to fetch actual price: HTTP error code : " + code);
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder responseStrBuilder = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                responseStrBuilder.append(responseLine.trim());
            }

            JsonNode responseJson = objectMapper.readTree(responseStrBuilder.toString());
            JsonNode dataNode = responseJson.path("data").path("historicalData");

            if (dataNode.isArray() && dataNode.size() > 0) {
                return dataNode.get(0).path("close").asDouble();
            } else {
                throw new RuntimeException("No actual price data found for date: " + date);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // As a last resort, return a default value or handle accordingly
            return 100.00; // Default fallback value
        }
    }

    /**
     * Prepares the Weka dataset from historical stock data.
     *
     * @param historicalData The list of historical stock data.
     * @return An Instances object representing the dataset.
     * @throws Exception If an error occurs during dataset preparation.
     */
    private Instances prepareDataset(List<StockData> historicalData) throws Exception {
        // Define attributes
        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("open"));
        attributes.add(new Attribute("high"));
        attributes.add(new Attribute("low"));
        attributes.add(new Attribute("volume"));
        attributes.add(new Attribute("close")); // Class attribute

        // Create dataset
        Instances dataset = new Instances("StockPrice", attributes, historicalData.size());
        dataset.setClassIndex(4); // 'close' is the target variable

        // Populate dataset
        for (StockData stock : historicalData) {
            double[] vals = new double[5];
            vals[0] = stock.getOpen();
            vals[1] = stock.getHigh();
            vals[2] = stock.getLow();
            vals[3] = stock.getVolume();
            vals[4] = stock.getClose();
            dataset.add(new DenseInstance(1.0, vals));
        }

        return dataset;
    }

    /**
     * Inner class to represent stock data.
     */
    private static class StockData {
        private String date;
        private double open;
        private double high;
        private double low;
        private double close;
        private double adjClose;
        private long volume;

        public StockData(String date, double open, double high, double low, double close, double adjClose, long volume) {
            this.date = date;
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
            this.adjClose = adjClose;
            this.volume = volume;
        }

        public String getDate() {
            return date;
        }

        public double getOpen() {
            return open;
        }

        public double getHigh() {
            return high;
        }

        public double getLow() {
            return low;
        }

        public double getClose() {
            return close;
        }

        public double getAdjClose() {
            return adjClose;
        }

        public long getVolume() {
            return volume;
        }
    }
}
