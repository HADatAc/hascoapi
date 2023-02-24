package module;

import com.google.inject.AbstractModule;
import play.Environment;

public class SecurityModule extends AbstractModule {

    private final com.typesafe.config.Config configuration;

    private final String baseUrl;

    public SecurityModule(final Environment environment, final com.typesafe.config.Config configuration) {
        this.configuration = configuration;
        this.baseUrl = configuration.getString("sirapi.console.host");
    }

}
