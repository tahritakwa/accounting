package fr.sparkit.accounting.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AmortizationTableDto {

    private Long idAssets;
    private Long fiscalYearId;
    private BigDecimal acquisitionValue;
    private BigDecimal previousDepreciation;
    private BigDecimal annuityExercise;
    private BigDecimal vcn;
}
