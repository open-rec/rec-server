package com.openrec.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestConfig {

    @Bean
    public RestTemplate restTemplate() {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        JdkClientHttpRequestFactory httpClientFactory = new JdkClientHttpRequestFactory(client);
        httpClientFactory.setReadTimeout(Duration.ofSeconds(60));
        return new RestTemplate(httpClientFactory);
    }
}
