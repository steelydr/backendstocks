package stocks.config;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AIConfig {
    
    @ConfigProperty(name = "ai.api.endpoint")
    String apiEndpoint;
    
    @ConfigProperty(name = "ai.api.key")
    String apiKey;
    
    @ConfigProperty(name = "ai.model", defaultValue = "gpt-3.5-turbo")
    String model;

    // Getters
    public String getApiEndpoint() {
        return apiEndpoint;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getModel() {
        return model;
    }
}