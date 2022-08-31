package fr.sparkit.accounting.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AnnexeReportDto {

    private String annexe;

    private String label;

    private String codeAccount;

    private String labelAccount;

    private String totalDebit;

    private String totalCredit;

}
