package stocks.models;

import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;

public class CompanyProfile {
    @JsonbProperty("name")
    private String name;
    
    @JsonbProperty("description")
    private String description;

    // Default no-args constructor
    public CompanyProfile() {
    }

    @JsonbCreator
    public CompanyProfile(
        @JsonbProperty("name") String name,
        @JsonbProperty("description") String description) {
        this.name = name;
        this.description = description;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    @Override
    public String toString() {
        return "CompanyProfile{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
