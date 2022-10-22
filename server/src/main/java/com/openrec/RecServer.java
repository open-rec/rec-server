package com.openrec;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RecServer {

    public static void main(String[] args){
        SpringApplication.run(RecServer.class, args);
    }
}
