package org.zerock.finance_dwpj1.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload-path}")
    private String uploadPath;

    @Value("${file.info-upload-path}")
    private String infoUploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // 콘텐츠리뷰: /uploads/** → C:/uploads/
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:///" + uploadPath + "/");

        // 정보 탭(Info): /info_uploads/** → C:/info_uploads/
        registry.addResourceHandler("/info_uploads/**")
                .addResourceLocations("file:///" + infoUploadPath + "/");
    }
}
