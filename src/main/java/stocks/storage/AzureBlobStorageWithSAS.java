package stocks.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;

import stocks.models.CompanyProfile;

public class AzureBlobStorageWithSAS {

    private static final String SAS_URL = "https://stockmldata.blob.core.windows.net/companydetails?sp=racwdl&st=2024-11-30T16:06:48Z&se=2025-12-01T00:06:48Z&sv=2022-11-02&sr=c&sig=rQGUOImPF8dw3JsqFwHdiyho3jp8n5gbMIIokHfDGHI%3D";

    private static BlobContainerClient getContainerClient() {
        // Create and return a BlobContainerClient using the SAS URL
        return new BlobContainerClientBuilder()
                .endpoint(SAS_URL)
                .buildClient();
    }

    public static void uploadCompanyProfile(String symbol, CompanyProfile profile) {
        // Get the container client
        BlobContainerClient containerClient = getContainerClient();

        // Convert the CompanyProfile to JSON
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonData;
        try {
            jsonData = objectMapper.writeValueAsString(profile);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error converting company profile to JSON");
        }

        // Generate the blob name
        String blobName = "companydetails_" + symbol.toUpperCase() + ".json";

        // Get a reference to the blob
        BlobClient blobClient = containerClient.getBlobClient(blobName);

        // Upload the JSON data as a blob
        try (InputStream dataStream = new ByteArrayInputStream(jsonData.getBytes())) {
            blobClient.upload(dataStream, jsonData.getBytes().length, true);
            System.out.println("Successfully uploaded: " + blobName);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error uploading to Azure Blob Storage using SAS URL");
        }
    }

    public static boolean doesProfileExist(String symbol) {
        // Get the container client
        BlobContainerClient containerClient = getContainerClient();

        // Generate the blob name
        String blobName = "companydetails_" + symbol.toUpperCase() + ".json";

        // Check if the blob exists
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        return blobClient.exists();
    }

    public static CompanyProfile getCompanyProfile(String symbol) {
        // Get the container client
        BlobContainerClient containerClient = getContainerClient();

        // Generate the blob name
        String blobName = "companydetails_" + symbol.toUpperCase() + ".json";

        // Get a reference to the blob
        BlobClient blobClient = containerClient.getBlobClient(blobName);

        // Download the blob content
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            blobClient.download(outputStream);
            String jsonData = outputStream.toString();

            // Convert JSON back to CompanyProfile object
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(jsonData, CompanyProfile.class);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error downloading or parsing company profile from Azure Blob Storage");
        }
    }
}
