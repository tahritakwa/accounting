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
public class StandardReportLineDto {
    private Long id;
    private String label;
    private String formula;
    private ReportType reportType;
    private String lineIndex;
    private String annexCode;
    private boolean isNegative;
    private boolean isTotal;
}
