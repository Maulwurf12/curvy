package de.dhbwravensburg.webeng.kilian.curvy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class BRouterConfiguration {

    @Bean
    RestClient bRouterRestClient(
            RestClient.Builder builder,
            @Value("${brouter.base-url}") String baseUrl
    ) {
        return builder.baseUrl(baseUrl).build();
    }
}
