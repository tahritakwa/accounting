package fr.sparkit.accounting.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
public class BankReconciliationPageDto {

    private BankReconciliationStatementDto bankReconciliationStatementDto;
    private Long total;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;

}
