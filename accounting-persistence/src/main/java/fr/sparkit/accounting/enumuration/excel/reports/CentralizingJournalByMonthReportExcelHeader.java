package fr.sparkit.accounting.enumuration.excel.reports;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum CentralizingJournalByMonthReportExcelHeader {
    JOURNAL_CODE("B"), JOURNAL_LABEL("C"), MONTH("D"), ACCOUNT_CODE("E"), ACCOUNT_LABEL("F"), DEBIT("G"), CREDIT("H");

    private String columnIndexCode;
}
