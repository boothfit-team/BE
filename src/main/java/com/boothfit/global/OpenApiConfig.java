package com.boothfit.global;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

    @Bean
    OpenAPI boothfitOpenApi() {
        return new OpenAPI().info(new Info()
                .title("BoothFit API")
                .version("0.1.0")
                .description("부스핏 행사 정보 API"));
    }
}
