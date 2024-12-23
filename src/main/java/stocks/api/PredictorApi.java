package stocks.api;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;

import io.smallrye.graphql.api.Context;
import jakarta.inject.Inject;
import stocks.services.PredictionResponse;
import stocks.services.PredictorService;

@GraphQLApi
public class PredictorApi {

    @Inject
    PredictorService predictorService;

    @Query("predictStockPrice")
    public CompletionStage<PredictionResponse> predictStockPrice(String symbol, String startDate, String endDate, Context context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return predictorService.predictStockPrice(symbol, startDate, endDate);
            } catch (Exception e) {
                // You might want to handle exceptions more gracefully
                PredictionResponse errorResponse = new PredictionResponse();
                errorResponse.setError("Error: " + e.getMessage());
                return errorResponse;
            }
        });
    }
}
