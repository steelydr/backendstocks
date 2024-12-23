package stocks.services;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PredictorModel {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    LocalDate date;
    double open, close, high, low, volume;

    public PredictorModel(LocalDate date, double open, double close, double high, double low, double volume) {
        this.date = date;
        this.open = open;
        this.close = close;
        this.high = high;
        this.low = low;
        this.volume = volume;
    }

    public static List<PredictorModel> parseResponse(String jsonResponse) throws Exception {
        List<PredictorModel> data = new ArrayList<>();
        int dataIndex = jsonResponse.indexOf("\"data\":");
        if (dataIndex == -1) return data;

        int histDataStart = jsonResponse.indexOf("\"historicalData\":", dataIndex);
        if (histDataStart == -1) return data;

        int arrayStart = jsonResponse.indexOf("[", histDataStart);
        if (arrayStart == -1) return data;

        int arrayEnd = findMatchingBracket(jsonResponse, arrayStart);
        if (arrayEnd == -1) return data;

        String historicalDataArray = jsonResponse.substring(arrayStart + 1, arrayEnd).trim();
        List<String> jsonObjects = splitJsonObjects(historicalDataArray);

        for (String day : jsonObjects) {
            String dateStr = extractJsonValue(day, "date");
            double open = parseNumericValue(extractJsonValue(day, "open"));
            double close = parseNumericValue(extractJsonValue(day, "close"));
            double high = parseNumericValue(extractJsonValue(day, "high"));
            double low = parseNumericValue(extractJsonValue(day, "low"));
            double volume = parseNumericValue(extractJsonValue(day, "volume"));

            data.add(new PredictorModel(LocalDate.parse(dateStr, DATE_FORMATTER), open, close, high, low, volume));
        }
        return data;
    }

    private static double parseNumericValue(String value) {
        return Double.parseDouble(value.replace(",", ""));
    }

    private static int findMatchingBracket(String str, int startIndex) {
        char openBracket = str.charAt(startIndex);
        char closeBracket = openBracket == '[' ? ']' : '}';
        int count = 1;
        for (int i = startIndex + 1; i < str.length(); i++) {
            char current = str.charAt(i);
            if (current == openBracket) count++;
            else if (current == closeBracket) count--;
            if (count == 0) return i;
        }
        return -1;
    }

    private static List<String> splitJsonObjects(String arrayStr) {
        List<String> objects = new ArrayList<>();
        int braceDepth = 0;
        int objStart = -1;
        for (int i = 0; i < arrayStr.length(); i++) {
            char c = arrayStr.charAt(i);
            if (c == '{') {
                if (braceDepth == 0) objStart = i;
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

    private static String extractJsonValue(String json, String key) throws Exception {
        String searchKey = "\"" + key + "\":";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) throw new Exception("Key not found: " + key);

        int valueStart = keyIndex + searchKey.length();
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) valueStart++;
        if (valueStart >= json.length()) throw new Exception("Value not found for key: " + key);

        char firstChar = json.charAt(valueStart);
        if (firstChar == '\"') {
            int endQuote = json.indexOf("\"", valueStart + 1);
            if (endQuote == -1) throw new Exception("Closing quote not found for key: " + key);
            return json.substring(valueStart + 1, endQuote);
        } else {
            int valueEnd = valueStart;
            while (valueEnd < json.length() && (Character.isDigit(json.charAt(valueEnd)) || json.charAt(valueEnd) == '.' || json.charAt(valueEnd) == ',')) {
                valueEnd++;
            }
            return json.substring(valueStart, valueEnd).replace(",", "");
        }
    }
}
