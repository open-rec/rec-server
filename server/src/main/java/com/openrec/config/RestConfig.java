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
        // rank-engine serves HTTP/1.1 (Uvicorn); do not send an h2c upgrade with a streaming POST.
        HttpClient client =
            HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(3)).build();
        JdkClientHttpRequestFactory httpClientFactory = new JdkClientHttpRequestFactory(client);
        httpClientFactory.setReadTimeout(Duration.ofSeconds(60));
        return new RestTemplate(httpClientFactory);
    }
}
