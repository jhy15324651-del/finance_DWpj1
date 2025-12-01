package org.zerock.finance_dwpj1.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties
@Getter
@Setter
public class StockKISConfig {
    private String appKey;
    private String appSecret;
    private String domain;
}
