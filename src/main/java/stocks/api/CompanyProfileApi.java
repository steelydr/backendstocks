package stocks.api;

import java.util.concurrent.TimeUnit;

import org.redisson.Redisson;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import stocks.models.CompanyProfile;
import stocks.scrappers.CompanyProfileScraper;
import stocks.storage.AzureBlobStorageWithSAS;

@Path("/api/stocks/profile")
public class CompanyProfileApi {

    private static final RedissonClient redissonClient;
    private static final RMapCache<String, CompanyProfile> cache;

    static {
        // Configure Redisson client
        Config config = new Config();
        config.useSingleServer().setAddress("redis://localhost:6379");
        redissonClient = Redisson.create(config);

        // Initialize the cache map
        cache = redissonClient.getMapCache("companyProfiles");
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public CompanyProfile getCompanyProfile(@QueryParam("symbol") String symbol) {
        if (symbol == null) {
            throw new IllegalArgumentException("Symbol is required");
        }

        String cacheKey = "companyprofile:" + symbol;

        // Check the cache
        if (cache.containsKey(cacheKey)) {
            System.out.println("Fetching company profile from Redis cache.");
            return cache.get(cacheKey);
        }

        CompanyProfile profile;

        // Check if the company profile exists in Azure Blob Storage
        if (AzureBlobStorageWithSAS.doesProfileExist(symbol)) {
            System.out.println("Fetching company profile from Azure Blob Storage.");
            profile = AzureBlobStorageWithSAS.getCompanyProfile(symbol);

            // Check for updates using the scraper
            CompanyProfile updatedProfile = CompanyProfileScraper.fetchCompanyProfile(symbol);
            if (!profile.equals(updatedProfile)) { // Assuming equals() is overridden in CompanyProfile
                System.out.println("Profile updated. Overwriting in Blob Storage and Redis.");
                
                // Overwrite the profile in Azure Blob Storage
                AzureBlobStorageWithSAS.uploadCompanyProfile(symbol, updatedProfile);

                // Overwrite in Redis cache
                cache.put(cacheKey, updatedProfile, 1, TimeUnit.DAYS);

                profile = updatedProfile;
            }
        } else {
            System.out.println("Fetching company profile using scraper.");
            profile = CompanyProfileScraper.fetchCompanyProfile(symbol);

            // Store the profile in Azure Blob Storage using SAS URL
            AzureBlobStorageWithSAS.uploadCompanyProfile(symbol, profile);
        }

        // Cache the profile in Redis with the updated key format for 1 day
        cache.put(cacheKey, profile, 1, TimeUnit.DAYS);

        return profile;
    }
}
