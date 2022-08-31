package fr.sparkit.accounting.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BankReconciliationStatementDto {

    private Long id;
    private int closeMonth;
    private Long fiscalYearId;
    private String fiscalYearLabel;
    private Long accountId;
    private String accountLabel;
    private int accountCode;
    private BigDecimal initialAmount;
    private BigDecimal finalAmount;
    private List<CloseDocumentAccountLineDto> closeDocumentAccountLines;
    private List<CloseDocumentAccountLineDto> documentAccountLinesAffected;
    private List<CloseDocumentAccountLineDto> documentAccountLinesReleased;

    public BankReconciliationStatementDto(int closeMonth, Long fiscalYearId, BigDecimal initialAmount,
            BigDecimal finalAmount) {
        super();
        this.closeMonth = closeMonth;
        this.fiscalYearId = fiscalYearId;
        this.initialAmount = initialAmount;
        this.finalAmount = finalAmount;
    }

    public BankReconciliationStatementDto(Long id, int closeMonth, Long fiscalYearId, String fiscalYearLabel,
            Long accountId, String accountLabel, int accountCode, BigDecimal initialAmount, BigDecimal finalAmount,
            List<CloseDocumentAccountLineDto> closeDocumentAccountLines) {
        super();
        this.id = id;
        this.closeMonth = closeMonth;
        this.fiscalYearId = fiscalYearId;
        this.fiscalYearLabel = fiscalYearLabel;
        this.accountId = accountId;
        this.accountLabel = accountLabel;
        this.accountCode = accountCode;
        this.initialAmount = initialAmount;
        this.finalAmount = finalAmount;
        this.closeDocumentAccountLines = closeDocumentAccountLines;
    }

}
