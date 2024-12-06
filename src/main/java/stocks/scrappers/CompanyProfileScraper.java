package stocks.scrappers;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import stocks.models.CompanyProfile;

public class CompanyProfileScraper {

    public static CompanyProfile fetchCompanyProfile(String symbol) {
        try {
            String url = String.format("https://finance.yahoo.com/quote/%s/profile", symbol);
            Document doc = Jsoup.connect(url).userAgent("Mozilla/5.0").get();
            
            Element nameElement = doc.selectFirst("#nimbus-app > section > section > section > article > section.container.yf-k4z9w > div.top.yf-k4z9w > div > div > section > h1");
            String name = nameElement != null ? nameElement.text() : "Name not found";

           Element descriptionElement = doc.selectFirst("#nimbus-app > section > section > section > article > section:nth-child(5) > section:nth-child(1) > p");
           String description = descriptionElement != null ? descriptionElement.text() : "Description not available";
            return new CompanyProfile(name, description);
        } catch (IOException e) {
            e.printStackTrace();
            return new CompanyProfile("Error fetching data", "Unable to retrieve company profile due to an I/O error.");
        }
    }
}
