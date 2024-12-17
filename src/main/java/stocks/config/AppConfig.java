package stocks.config;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AppConfig {

    @Inject
    DevUIConfigModifier devUIConfigModifier;

    @PostConstruct
    public void init() {
        devUIConfigModifier.configureDevUI();
    }
}
