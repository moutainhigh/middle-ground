package com.cloud.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * swagger文档
 * 
 * @author 关键
 *
 */
@Configuration
@EnableSwagger2
public class SwaggerConfig {

	@Bean
	public Docket docket() {
		return new Docket(DocumentationType.SWAGGER_2).groupName("通知中心swagger接口文档")
				.apiInfo(new ApiInfoBuilder().title("通知中心swagger接口文档")
						.contact(new Contact("关键", "", "27958478@qq.com")).version("1.0").build())
				.select().paths(PathSelectors.any()).build();
	}
}
