package org.zerock.finance_dwpj1.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Component
@ConfigurationProperties(prefix = "kis")
@Getter
@Setter
public class StockKisConfig {
    private String appKey;
    private String appSecret;
    private String domain;
}
