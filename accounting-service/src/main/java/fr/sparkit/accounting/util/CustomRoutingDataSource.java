package fr.sparkit.accounting.util;

import java.util.HashMap;
import java.util.Map;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;

public class CustomRoutingDataSource extends AbstractRoutingDataSource {
    private static Map<Object, Object> companiesDataSources;
    private static DataSource defaultCompanyDataSource;
    @Override
    protected Object determineCurrentLookupKey() {
        return CompanyContextHolder.getCompanyContext();
    }

    public void initDatasource(Object defaultDataSource, Map<Object, Object> dataSource) {
        this.setTargetDataSources(dataSource);
        companiesDataSources=dataSource;
        defaultCompanyDataSource = (DataSource) defaultDataSource;
        this.setDefaultTargetDataSource(defaultDataSource);
        this.afterPropertiesSet();
    }
    @Override
    protected DataSource determineTargetDataSource() {
        Object lookupKey = determineCurrentLookupKey();
        DataSource dataSource = (DataSource) companiesDataSources.get(lookupKey);
        if(dataSource==null){
            dataSource = defaultCompanyDataSource;
        }
        if (dataSource == null) {
            throw new IllegalStateException("Cannot determine target DataSource for lookup key [" + lookupKey + "]");
        }
        return dataSource;
    }
}
