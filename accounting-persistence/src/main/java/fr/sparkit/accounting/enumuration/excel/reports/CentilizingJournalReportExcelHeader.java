package fr.sparkit.accounting.enumuration.excel.reports;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum CentilizingJournalReportExcelHeader {
    ACCOUNT_CODE("B"), ACCOUNT_LABEL("C"), DEBIT("D"), CREDIT("E");

    private String columnIndexCode;
}
