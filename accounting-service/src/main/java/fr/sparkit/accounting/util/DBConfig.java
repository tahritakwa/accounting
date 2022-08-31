package fr.sparkit.accounting.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import fr.sparkit.accounting.entities.multitenancy.DBComptaConfig;
import fr.sparkit.accounting.entities.multitenancy.DBComptaConfigRowMapper;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class DBConfig {
    @Value("${env.name}")
    private String envName;
    @Value("${env.module}")
    private String module;
    private static final String PROPERTY_NOT_FOUND = "Property %s not found";

    private final Environment env;

    public DBConfig(Environment env) {
        this.env = env;
    }

    @Bean()
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public DataSource dataSource() {
        CompanyContextHolder.supportedCompanies.clear();
        log.info("initializing databases for env.name : {} and module {}", envName, module);
        Map<Object, Object> companiesDataSource = new HashMap<>();
        Map<String, Object> applicationProperties = new HashMap<>();
        MutablePropertySources mutablePropertySources = ((AbstractEnvironment) env).getPropertySources();
        for (PropertySource<?> mutablePropertySource : mutablePropertySources) {
            if (mutablePropertySource instanceof MapPropertySource) {
                applicationProperties.putAll(((MapPropertySource) mutablePropertySource).getSource());
            }
        }

        DataSource masterDataSource = initMasterDataSource(
                Objects.requireNonNull(applicationProperties.get("spring.datasource.master.url"),
                        String.format(PROPERTY_NOT_FOUND, "spring.datasource.master.url")).toString(),
                Objects.requireNonNull(applicationProperties.get("spring.datasource.master.username"),
                        String.format(PROPERTY_NOT_FOUND, "spring.datasource.master.username")).toString(),
                Objects.requireNonNull(applicationProperties.get("spring.datasource.master.password"),
                        String.format(PROPERTY_NOT_FOUND, "spring.datasource.master.password")).toString(),
                Objects.requireNonNull(applicationProperties.get("spring.datasource.master.driverClassName"),
                        String.format(PROPERTY_NOT_FOUND, "spring.datasource.master.driverClassName")).toString());

        JdbcTemplate jdbcTemplate = new JdbcTemplate(masterDataSource);
        List<DBComptaConfig> dataSourcesInfo = jdbcTemplate.query(
                "SELECT * FROM DBComptaConfig WHERE env=? AND module=?", new Object[] { envName, module },
                new DBComptaConfigRowMapper());
        for (DBComptaConfig comptaConfig : dataSourcesInfo) {
            log.info("data source for company {} ({},{},{},{})", comptaConfig.getCompanyCode(), comptaConfig.getUrl(),
                    comptaConfig.getUsername(), comptaConfig.getPassword(), comptaConfig.getDriverClassName());
            CompanyContextHolder.supportedCompanies.add(comptaConfig.getCompanyCode());
            DataSource dataSource = getDataSourceFromCompanyProperties(comptaConfig.getUrl(),
                    comptaConfig.getUsername(), comptaConfig.getPassword(), comptaConfig.getDriverClassName());
            companiesDataSource.put(comptaConfig.getCompanyCode(), dataSource);
        }
        CustomRoutingDataSource customDataSource = new CustomRoutingDataSource();
        customDataSource.initDatasource(companiesDataSource.get(dataSourcesInfo.get(0).getCompanyCode()),
                companiesDataSource);
        return customDataSource;
    }

    private static DataSource initMasterDataSource(String url, String userName, String password,
                                                   String driverClassName) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(url);
        dataSource.setUsername(userName);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driverClassName);
        return dataSource;
    }

    private DataSource getDataSourceFromCompanyProperties(String url, String userName, String password,
                                                          String driverClassName) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(url);
        dataSource.setUsername(userName);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driverClassName);
        if (env.getActiveProfiles().length == 0 || !"test".equals(env.getActiveProfiles()[0])) {
            Flyway flyway = Flyway.configure().dataSource(dataSource).load();
            Flyway.configure();
            flyway.migrate();
        }
        return dataSource;
    }
}
