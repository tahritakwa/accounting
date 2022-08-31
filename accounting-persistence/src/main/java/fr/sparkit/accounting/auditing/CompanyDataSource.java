package fr.sparkit.accounting.auditing;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyDataSource {
    private String diverClassName;
    private String url;
    private String username;
    private String password;
}
