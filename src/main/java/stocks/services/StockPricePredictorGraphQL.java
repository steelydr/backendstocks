package stocks.services;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;

import weka.classifiers.functions.LinearRegression;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

public class StockPricePredictorGraphQL {
    private static final String GRAPHQL_ENDPOINT = "http://192.168.40.86:80/graphql";
    // Updated date formatter to accept single-digit days
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    /**
     * Represents a single day's stock data.
     */
    static class StockData {
        LocalDate date;
        double open, close, high, low, volume;

        /**
         * Constructs a StockData object by parsing the date string.
         *
         * @param dateStr The date string in "MMM d, yyyy" format.
         * @param open    Opening price.
         * @param close   Closing price.
         * @param high    Highest price of the day.
         * @param low     Lowest price of the day.
         * @param volume  Trading volume.
         * @throws Exception If the date string cannot be parsed.
         */
        public StockData(String dateStr, double open, double close, double high, double low, double volume) throws Exception {
            try {
                this.date = LocalDate.parse(dateStr, DATE_FORMATTER);
            } catch (Exception e) {
                throw new Exception("Invalid date format: " + dateStr);
            }
            this.open = open;
            this.close = close;
            this.high = high;
            this.low = low;
            this.volume = volume;
        }
    }

    public static void main(String[] args) {
        String symbol = "TSLA"; // Hardcoded stock symbol

        try {
            System.out.println("\n=== Processing " + symbol + " ===");
            predictStockPrice(symbol);
        } catch (Exception e) {
            System.out.println("Error processing " + symbol + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Fetches historical data, trains the regression model, and makes a prediction.
     *
     * @param symbol The stock symbol to process.
     * @throws Exception If any error occurs during processing.
     */
    private static void predictStockPrice(String symbol) throws Exception {
        List<StockData> historicalData = fetchHistoricalData(symbol);

        if (historicalData.isEmpty()) {
            System.out.println("No data received for " + symbol);
            return;
        }

        // Create attributes for the dataset
        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("open"));
        attributes.add(new Attribute("high"));
        attributes.add(new Attribute("low"));
        attributes.add(new Attribute("volume"));
        attributes.add(new Attribute("prev_close"));
        attributes.add(new Attribute("close")); // Target variable

        // Create dataset
        Instances dataset = new Instances("StockData", attributes, historicalData.size());
        dataset.setClassIndex(5); // 'close' is the target variable

        // Add instances to the dataset
        for (int i = 1; i < historicalData.size(); i++) {
            StockData current = historicalData.get(i);
            StockData previous = historicalData.get(i - 1);

            double[] values = new double[6];
            values[0] = current.open;
            values[1] = current.high;
            values[2] = current.low;
            values[3] = current.volume;
            values[4] = previous.close;
            values[5] = current.close;

            dataset.add(new DenseInstance(1.0, values));
        }

        // Train model
        LinearRegression model = new LinearRegression();
        model.buildClassifier(dataset);

        // Prepare prediction data
        StockData lastDay = historicalData.get(historicalData.size() - 1);
        double[] predictionValues = new double[6];
        predictionValues[0] = lastDay.open;
        predictionValues[1] = lastDay.high;
        predictionValues[2] = lastDay.low;
        predictionValues[3] = lastDay.volume;
        predictionValues[4] = lastDay.close;
        predictionValues[5] = 0; // Placeholder for the target variable

        DenseInstance predictionInstance = new DenseInstance(1.0, predictionValues);
        predictionInstance.setDataset(dataset);

        // Make prediction
        double predictedClose = model.classifyInstance(predictionInstance);

        // Print results with more detailed information
        System.out.println("Symbol: " + symbol);
        System.out.println("Last known date: " + lastDay.date.format(DATE_FORMATTER));
        System.out.println("Last closing price: $" + String.format("%.2f", lastDay.close));
        System.out.println("Predicted next closing price: $" + String.format("%.2f", predictedClose));
        System.out.println("Predicted change: " + String.format("%.2f%%",
                ((predictedClose - lastDay.close) / lastDay.close) * 100));
        System.out.println("\nModel Details:");
        System.out.println(model);
    }

    /**
     * Fetches historical stock data from the GraphQL endpoint.
     *
     * @param symbol The stock symbol to fetch data for.
     * @return A list of StockData objects sorted chronologically.
     * @throws Exception If any error occurs during data fetching or parsing.
     */
    private static List<StockData> fetchHistoricalData(String symbol) throws Exception {
        List<StockData> data = new ArrayList<>();

        String query = String.format("""
                {
                  historicalData(symbol: "%s", startDate: "2022-01-01", endDate: "2024-12-19") {
                    date
                    open
                    close
                    high
                    low
                    volume
                  }
                }
                """, symbol);

        String jsonRequest = "{ \"query\": \"" + escapeQuotes(query) + "\" }";

        Response response = Jsoup.connect(GRAPHQL_ENDPOINT)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .requestBody(jsonRequest)
                .method(Connection.Method.POST)
                .ignoreContentType(true)
                .execute();

        String responseBody = response.body();

        int dataIndex = responseBody.indexOf("\"data\":");
        if (dataIndex == -1) {
            System.out.println("Error in response for " + symbol + ": " + responseBody);
            return data;
        }

        int histDataStart = responseBody.indexOf("\"historicalData\":", dataIndex);
        if (histDataStart == -1) {
            System.out.println("No historicalData found for " + symbol);
            return data;
        }

        int arrayStart = responseBody.indexOf("[", histDataStart);
        int arrayEnd = findMatchingBracket(responseBody, arrayStart);
        if (arrayStart == -1 || arrayEnd == -1) {
            System.out.println("Malformed historicalData array for " + symbol);
            return data;
        }

        String historicalDataArray = responseBody.substring(arrayStart + 1, arrayEnd).trim();
        List<String> jsonObjects = splitJsonObjects(historicalDataArray);

        for (String day : jsonObjects) {
            try {
                String date = extractJsonValue(day, "date");
                double open = parseNumericValue(extractJsonValue(day, "open"));
                double close = parseNumericValue(extractJsonValue(day, "close"));
                double high = parseNumericValue(extractJsonValue(day, "high"));
                double low = parseNumericValue(extractJsonValue(day, "low"));
                double volume = parseNumericValue(extractJsonValue(day, "volume"));

                data.add(new StockData(date, open, close, high, low, volume));
            } catch (Exception e) {
                System.out.println("Warning: Skipping data point for " + symbol + " due to parsing error: " + e.getMessage());
            }
        }

        // Sort the data chronologically (ascending order)
        Collections.sort(data, Comparator.comparing(a -> a.date));

        System.out.println("Retrieved " + data.size() + " data points for " + symbol);
        if (!data.isEmpty()) {
            System.out.println("Date range: " + data.get(0).date.format(DATE_FORMATTER) + " to " + data.get(data.size() - 1).date.format(DATE_FORMATTER));
            System.out.println("\nRaw response from server:");
            System.out.println(responseBody.substring(0, Math.min(500, responseBody.length())) + "...");
        }
        return data;
    }

    /**
     * Parses a numeric value from a string, removing any commas.
     *
     * @param value The string to parse.
     * @return The parsed double value.
     */
    private static double parseNumericValue(String value) {
        return Double.parseDouble(value.replace(",", ""));
    }

    /**
     * Escapes double quotes in a string by prefixing them with a backslash.
     *
     * @param input The input string.
     * @return The escaped string.
     */
    private static String escapeQuotes(String input) {
        return input.replace("\"", "\\\"");
    }

    /**
     * Finds the index of the matching closing bracket for the opening bracket at startIndex.
     *
     * @param str        The string to search within.
     * @param startIndex The index of the opening bracket.
     * @return The index of the matching closing bracket, or -1 if not found.
     */
    private static int findMatchingBracket(String str, int startIndex) {
        if (startIndex < 0 || startIndex >= str.length()) {
            return -1;
        }
        char openBracket = str.charAt(startIndex);
        char closeBracket;
        if (openBracket == '[') {
            closeBracket = ']';
        } else if (openBracket == '{') {
            closeBracket = '}';
        } else {
            return -1;
        }
        int count = 1;
        for (int i = startIndex + 1; i < str.length(); i++) {
            char current = str.charAt(i);
            if (current == openBracket) {
                count++;
            } else if (current == closeBracket) {
                count--;
                if (count == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Splits a JSON array string into individual JSON object strings.
     *
     * @param arrayStr The JSON array string without the enclosing brackets.
     * @return A list of JSON object strings.
     */
    private static List<String> splitJsonObjects(String arrayStr) {
        List<String> objects = new ArrayList<>();
        int braceDepth = 0;
        int objStart = -1;
        for (int i = 0; i < arrayStr.length(); i++) {
            char c = arrayStr.charAt(i);
            if (c == '{') {
                if (braceDepth == 0) {
                    objStart = i;
                }
                braceDepth++;
            } else if (c == '}') {
                braceDepth--;
                if (braceDepth == 0 && objStart != -1) {
                    objects.add(arrayStr.substring(objStart, i + 1));
                    objStart = -1;
                }
            }
        }
        return objects;
    }

    /**
     * Extracts the value associated with a given key in a JSON object string.
     *
     * @param json The JSON object string.
     * @param key  The key whose value needs to be extracted.
     * @return The extracted value as a string.
     * @throws Exception If the key is not found or the value cannot be parsed.
     */
    private static String extractJsonValue(String json, String key) throws Exception {
        String searchKey = "\"" + key + "\":";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) {
            throw new Exception("Key \"" + key + "\" not found.");
        }
        int valueStart = keyIndex + searchKey.length();
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }
        if (valueStart >= json.length()) {
            throw new Exception("No value found for key \"" + key + "\".");
        }
        char firstChar = json.charAt(valueStart);
        if (firstChar == '\"') {
            int endQuote = json.indexOf("\"", valueStart + 1);
            if (endQuote == -1) {
                throw new Exception("Unterminated string value for key \"" + key + "\".");
            }
            return json.substring(valueStart + 1, endQuote);
        } else {
            int valueEnd = valueStart;
            while (valueEnd < json.length() &&
                   (Character.isDigit(json.charAt(valueEnd)) ||
                    json.charAt(valueEnd) == '.' ||
                    json.charAt(valueEnd) == '-' ||
                    json.charAt(valueEnd) == ',')) {
                valueEnd++;
            }
            return json.substring(valueStart, valueEnd);
        }
    }
}
