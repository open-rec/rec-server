package com.openrec;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import springfox.documentation.oas.annotations.EnableOpenApi;


@EnableOpenApi
@SpringBootApplication
public class RecServer {

    public static void main(String[] args) {
        SpringApplication.run(RecServer.class, args);
    }
}
