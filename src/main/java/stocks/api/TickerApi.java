package stocks.api;

import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;

import jakarta.inject.Inject;
import stocks.models.TickerData;
import stocks.scrappers.TickerDetailsScraper;

@GraphQLApi
public class TickerApi {

    @Inject
    TickerDetailsScraper tickerDetailsScraper;

    @Query("getTickerDetails")
    @Description("Fetches detailed information for a given stock ticker symbol.")
    public TickerData getTickerDetails(String symbol) {
        return tickerDetailsScraper.fetchTickerDetails(symbol.toUpperCase());
    }
}
