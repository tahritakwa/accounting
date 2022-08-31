package fr.sparkit.accounting.enumuration.excel.reports;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum StateOfJournalsReportExcelHeader {
    DOCUMENT_CODE("B"), DOCUMENT_DATE("C"), LABEL("D"), AMOUNT("E");

    private String columnIndexCode;
}
