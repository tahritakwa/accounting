package fr.sparkit.accounting.application;

import java.util.Locale;

import javax.annotation.Resource;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import fr.sparkit.accounting.services.impl.DocumentAccountAttachmentService;
import lombok.extern.slf4j.Slf4j;

@Configuration
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
@EntityScan("fr.sparkit.accounting.entities")
@ComponentScan("fr.sparkit.accounting")
@EnableJpaRepositories("fr.sparkit.accounting")
@EnableCaching
@Slf4j
public class MainApplication extends SpringBootServletInitializer implements CommandLineRunner {
    @Resource
    private DocumentAccountAttachmentService storageService;

    public MainApplication() {
        super();
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(MainApplication.class);
    }

    public static void main(String[] args) {
        log.info("Starting {} application...\", \"accounting-back-end-java\"");
        SpringApplication.run(MainApplication.class, args);
    }

    @Override
    public void run(String[] args) {
        storageService.init();
        log.info("Document account files storage directory created");
    }

    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver slr = new SessionLocaleResolver();
        slr.setDefaultLocale(Locale.FRANCE);
        return slr;
    }

}
