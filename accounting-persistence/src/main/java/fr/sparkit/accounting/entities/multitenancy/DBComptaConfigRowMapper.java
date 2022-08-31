package fr.sparkit.accounting.entities.multitenancy;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

public class DBComptaConfigRowMapper implements RowMapper {
    @Override
    public Object mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        return new DBComptaConfig(resultSet.getInt("id"), resultSet.getString("url"), resultSet.getString("username"),
                resultSet.getString("password"), resultSet.getString("driverClassName"),
                resultSet.getString("companyCode"), resultSet.getString("env"), resultSet.getString("module"));
    }
}
