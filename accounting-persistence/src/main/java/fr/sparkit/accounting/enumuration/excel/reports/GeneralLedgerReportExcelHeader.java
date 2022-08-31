package fr.sparkit.accounting.enumuration.excel.reports;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum GeneralLedgerReportExcelHeader {
    DATE("B"), JOURNAL_LABEL("C"), DOCUMENT_LABEL("D"), DOCUMENT_CODE("E"), DEBIT("F"), CREDIT("G"), AMOUNT("H");

    private String columnIndexCode;
}
