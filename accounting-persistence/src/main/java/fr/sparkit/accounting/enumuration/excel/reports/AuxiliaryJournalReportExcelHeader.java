package fr.sparkit.accounting.enumuration.excel.reports;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum AuxiliaryJournalReportExcelHeader {
    DOCUMENT_DATE("B"), DOCUMENT_CODE("C"), DOCUMENT_LINE_DATE("D"), ACCOUNT_CODE("E"), LABEL("F"), DEBIT("G"), CREDIT(
            "H");

    private String columnIndexCode;
}
