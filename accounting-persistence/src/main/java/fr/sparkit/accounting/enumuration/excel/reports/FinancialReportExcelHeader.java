package fr.sparkit.accounting.enumuration.excel.reports;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum FinancialReportExcelHeader {
    LABEL("B"), ANNEX("C"), CURRENT_FISCAL_YEAR("D"), PREVIOUS_FISCAL_YEAR("E");

    private String columnIndexCode;
}
