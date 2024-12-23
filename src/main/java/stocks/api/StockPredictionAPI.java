package stocks.api;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;

import stocks.services.MachineLearningService;

@GraphQLApi
@ApplicationScoped
public class StockPredictionAPI {

    @Inject
    MachineLearningService machineLearningService;

    /**
     * GraphQL Query to get the predicted next day price for a given symbol.
     * 
     * Example Query:
     * 
     * query {
     *   predictedNextDayPrice(symbol: "AAPL")
     * }
     *
     * @param symbol The stock symbol.
     * @return The predicted next day's stock price.
     */
    @Query("predictedNextDayPrice")
    public double getPredictedNextDayPrice(String symbol) {
        return machineLearningService.predictNextDayPrice(symbol);
    }
}
