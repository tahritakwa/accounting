package fr.sparkit.accounting.enumuration.excel.reports;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum AmortizationTableReportExcelHeader {
    DESIGNATION("B"), RATE("C"), DATE_OF_COMMISSIONING("D"), ACQUISITION_VALUE("E"), PREVIOUS_DEPRECIATION(
            "F"), ANNUITY_EXERCISE("G"), VCN("H");

    private String columnIndexCode;
}
