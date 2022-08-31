package fr.sparkit.accounting.application;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import fr.sparkit.accounting.util.DataSourceInterceptor;
import fr.sparkit.accounting.util.LanguageInterceptorService;
import fr.sparkit.accounting.util.UserInterceptorService;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final DataSourceInterceptor dataSourceInterceptor;
    private final LanguageInterceptorService languageInterceptorService;
    private final UserInterceptorService userInterceptorService;
    private static final String API = "/api/**";

    public WebConfig(DataSourceInterceptor dataSourceInterceptor, LanguageInterceptorService languageInterceptorService,
            UserInterceptorService userInterceptorService) {
        this.dataSourceInterceptor = dataSourceInterceptor;
        this.languageInterceptorService = languageInterceptorService;
        this.userInterceptorService = userInterceptorService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(dataSourceInterceptor).addPathPatterns(API);
        registry.addInterceptor(languageInterceptorService).addPathPatterns(API);
        registry.addInterceptor(userInterceptorService).addPathPatterns(API);
        WebMvcConfigurer.super.addInterceptors(registry);
    }
}
