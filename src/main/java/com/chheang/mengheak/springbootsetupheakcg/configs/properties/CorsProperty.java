package com.chheang.mengheak.springbootsetupheakcg.configs.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "application.cors")
@Getter
@Setter
public class CorsProperty {
    private List<String> allowedOrigins;
}
