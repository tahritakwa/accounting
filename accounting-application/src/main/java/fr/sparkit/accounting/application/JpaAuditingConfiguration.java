package fr.sparkit.accounting.application;

import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.web.context.request.RequestContextHolder;

import fr.sparkit.accounting.constants.AccountingConstants;

@Configuration
@EnableAspectJAutoProxy
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfiguration {
    private final Environment environment;

    @Autowired
    public JpaAuditingConfiguration(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.ofNullable(getCurrentUser());
    }

    /*
     * don't remove test for 'RequestContextHolder.getRequestAttributes() != null'
     */
    private String getCurrentUser() {
        if (environment.getActiveProfiles().length > 0 && environment.getActiveProfiles()[0] != null
                && environment.getActiveProfiles()[0].equals(AccountingConstants.TEST_PROFILE)) {
            return AccountingConstants.DEFAULT_USER_TEST_ID;
        } else if (RequestContextHolder.getRequestAttributes() != null && RequestContextHolder.getRequestAttributes()
                .getAttribute(AccountingConstants.USER_ID_HEADER, 0) != null) {
            return Objects
                    .requireNonNull(RequestContextHolder.getRequestAttributes()
                            .getAttribute(AccountingConstants.USER_ID_HEADER, 0))
                    .toString();
        } else {
            return AccountingConstants.DEFAULT_USER_SWAGGER_ID;
        }
    }
}
