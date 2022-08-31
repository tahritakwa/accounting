package fr.sparkit.accounting.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AmortizationTableReportDto {

    private String accountLabel;
    private String designation;
    private String rate;
    private String dateOfCommissioning;
    private String acquisitionValue;
    private String previousDepreciation;
    private String annuityExercise;
    private String vcn;
}
