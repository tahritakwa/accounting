package fr.sparkit.accounting.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TrialBalanceReportLineDto {
    private String code;
    private String accountLabel;
    private String totalInitialDebit;
    private String totalInitialCredit;
    private String totalCurrentDebit;
    private String totalCurrentCredit;
    private String accumulatedDebit;
    private String accumulatedCredit;
    private String balanceDebit;
    private String balanceCredit;
}
