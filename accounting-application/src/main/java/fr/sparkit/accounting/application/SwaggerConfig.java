package fr.sparkit.accounting.application;

import static fr.sparkit.accounting.constants.AccountingConstants.COMPANY_HEADER_NAME;
import static fr.sparkit.accounting.constants.AccountingConstants.LANGUAGE_HEADER_NAME;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import springfox.bean.validators.configuration.BeanValidatorPluginsConfiguration;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.Parameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
@Import(BeanValidatorPluginsConfiguration.class)
public class SwaggerConfig {
    @Bean
    public Docket api() {
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(new ParameterBuilder().name(COMPANY_HEADER_NAME).modelRef(new ModelRef("string"))
                .parameterType("header").required(true).build());
        parameters.add(new ParameterBuilder().name(LANGUAGE_HEADER_NAME).modelRef(new ModelRef("string"))
                .parameterType("header").required(true).build());
        return new Docket(DocumentationType.SWAGGER_2).select().apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.any()).build().globalOperationParameters(parameters);
    }
}
