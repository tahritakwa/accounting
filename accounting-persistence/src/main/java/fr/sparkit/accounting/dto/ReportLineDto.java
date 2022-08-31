package fr.sparkit.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import fr.sparkit.accounting.auditing.MoneySerializer;
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
public class ReportLineDto {

    private Long id;
    private String label;
    private String formula;
    private ReportType reportType;
    private String lineIndex;
    private String annexCode;
    private String previousFiscalYear;

    @JsonSerialize(using = MoneySerializer.class)
    private BigDecimal previousFiscalYearAmount;

    private FiscalYearDto fiscalYear;

    @JsonSerialize(using = MoneySerializer.class)
    private BigDecimal amount;
    private String user;
    private LocalDateTime lastUpdated;
    private boolean isNegative;
    private boolean isManuallyChanged;
    private boolean isTotal;

}
