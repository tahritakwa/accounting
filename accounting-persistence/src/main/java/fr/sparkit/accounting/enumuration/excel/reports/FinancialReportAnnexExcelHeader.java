package fr.sparkit.accounting.enumuration.excel.reports;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum FinancialReportAnnexExcelHeader {
    ANNEX("B"), LABEL("C"), ACCOUNT_CODE("D"), ACCOUNT_LABEL("E"), DEBIT("F"), CREDIT("G");

    private String columnIndexCode;
}
