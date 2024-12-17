package stocks.config;
import org.eclipse.microprofile.config.Config;

import jakarta.inject.Inject;
@jakarta.enterprise.context.ApplicationScoped
public class DevUIConfigModifier {

    @Inject
    Config config;

    public void configureDevUI() {
        // Modify the Dev UI configuration dynamically
        String devUIHost = "192.168.40.86"; // Allow remote access

        // Apply the configuration programmatically
        System.setProperty("quarkus.dev.ui.host", devUIHost);
    }
}
