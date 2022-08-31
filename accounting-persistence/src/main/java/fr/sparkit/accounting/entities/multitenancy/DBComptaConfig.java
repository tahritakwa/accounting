package fr.sparkit.accounting.entities.multitenancy;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DBComptaConfig {
    Integer id;
    private String url;
    private String username;
    private String password;
    private String driverClassName;
    private String companyCode;
    private String envName;
    private String module;

}
