package com.openrec.config;

import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;

@Configuration
public class RestConfig {

    @Bean
    public RestTemplate restTemplate() {
        OkHttp3ClientHttpRequestFactory httpClientFactory = new OkHttp3ClientHttpRequestFactory(
            new OkHttpClient.Builder().connectionPool(new ConnectionPool(1024, 60, TimeUnit.SECONDS)).build());
        httpClientFactory.setConnectTimeout(3000);
        httpClientFactory.setReadTimeout(60000);
        return new RestTemplate(httpClientFactory);
    }
}
