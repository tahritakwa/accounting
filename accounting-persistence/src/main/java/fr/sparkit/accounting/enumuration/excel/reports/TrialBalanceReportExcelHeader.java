package fr.sparkit.accounting.enumuration.excel.reports;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TrialBalanceReportExcelHeader {
    ACCOUNT_CODE("B"), ACCOUNT_LABEL("C"), CUMULATIVE_PERIOD_DEBIT("D"), CUMULATIVE_PERIOD_CREDIT("E"), PERIOD_DEBIT(
            "F"), PERIOD_CREDIT("G");

    private String columnIndexCode;
}
