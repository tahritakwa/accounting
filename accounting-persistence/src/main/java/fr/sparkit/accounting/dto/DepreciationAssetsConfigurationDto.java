package fr.sparkit.accounting.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DepreciationAssetsConfigurationDto {
    private Long id;
    private long idCategory;
    private int depreciationPeriod;
    private long immobilizationAccount;
    private long amortizationAccount;
    private String immobilisationTypeText;

}
