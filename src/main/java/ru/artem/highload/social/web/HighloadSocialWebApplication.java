package ru.artem.highload.social.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class HighloadSocialWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(HighloadSocialWebApplication.class, args);
    }

}
