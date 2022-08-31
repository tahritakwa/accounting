package fr.sparkit.accounting.dto;

import fr.sparkit.accounting.enumuration.ReportType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class PrintableReportLineDto {

    private Long id;
    private String label;
    private ReportType reportType;
    private String lineIndex;
    private String annexCode;
    private String previousFiscalYear;
    private String previousFiscalYearAmount;
    private FiscalYearDto fiscalYear;
    private String amount;

    public PrintableReportLineDto(String label) {
        this.label = label;
    }

    public PrintableReportLineDto(String label, String amount) {
        this.label = label;
        this.amount = amount;
    }

}
