package fr.sparkit.accounting.services.utils;

import fr.sparkit.accounting.dto.DataBaseDto;
import fr.sparkit.accounting.entities.multitenancy.DBComptaConfig;
import fr.sparkit.accounting.entities.multitenancy.DBComptaConfigRowMapper;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import javax.sql.DataSource;
import java.util.List;

@Service
@Slf4j
public final class DBManagementUtil {

    @Value("${spring.datasource.master.url}")
    private String databaseUrl;

    @Value("${spring.datasource.master.username}")
    private String dataBaseUsername;

    @Value("${spring.datasource.master.password}")
    private String dataBasePassword;

    @Value("${spring.datasource.master.driverClassName}")
    private String dataBaseClassDriver;

    @Value("${env.name}")
    private String envName;
    @Value("${env.module}")
    private String module;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public String createDataBase(@RequestBody DataBaseDto dataBaseDto){
        try{
            log.info("Trying to create new database with name : {} for company with code : {}",dataBaseDto.getDataBaseName(),dataBaseDto.getCompanyCode());
            createDataBase(dataBaseDto.getDataBaseName());
            migrate(dataBaseDto.getCompanyCode());
            return "Creation and migration of "+dataBaseDto.getDataBaseName()+"database , for company with code : "+dataBaseDto.getCompanyCode()+" have been successfully completed.";
        }catch (Exception e){
            return e.getMessage();
        }
    }

    private void createDataBase(String dataBaseName){
        log.info("Database creation...");
        jdbcTemplate.setDataSource(dataSource(databaseUrl.split(";")[0],dataBaseUsername,dataBasePassword,dataBaseClassDriver));
        jdbcTemplate.execute("CREATE DATABASE "+dataBaseName);
    }

    private void migrate(String companyCode){
        log.info("Database migration....");
        jdbcTemplate.setDataSource(dataSource(databaseUrl,dataBaseUsername,dataBasePassword,dataBaseClassDriver));
        List<DBComptaConfig> dataSourcesInfo = jdbcTemplate.query(
                "SELECT * FROM DBComptaConfig WHERE env=? AND module=? AND companyCode=?", new Object[] { envName, module, companyCode },
                new DBComptaConfigRowMapper());
        DBComptaConfig dbComptaConfig = dataSourcesInfo.get(0);
        DataSource dataSource = dataSource(dbComptaConfig.getUrl(),dbComptaConfig.getUsername(),dbComptaConfig.getPassword(),dbComptaConfig.getDriverClassName());
        Flyway flyway = Flyway.configure().dataSource(dataSource).load();
        Flyway.configure();
        flyway.migrate();
    }
    private DataSource dataSource(String url , String username , String password , String driverClass){
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driverClass);
        return dataSource;
    }
}
