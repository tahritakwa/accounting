package fr.sparkit.accounting.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import fr.sparkit.accounting.auditing.MoneySerializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GeneralLedgerAccountDto {
    private Long accountId;
    private int accountCode;
    private String accountName;

    @JsonSerialize(using = MoneySerializer.class)
    private BigDecimal totalDebit;

    @JsonSerialize(using = MoneySerializer.class)
    private BigDecimal totalCredit;

    @JsonSerialize(using = MoneySerializer.class)
    private BigDecimal totalBalance;
    
    private boolean isLiterable;

    public GeneralLedgerAccountDto(BigDecimal totalDebit, BigDecimal totalCredit, BigDecimal totalBalance) {
        super();
        this.totalDebit = totalDebit;
        this.totalCredit = totalCredit;
        this.totalBalance = totalBalance;
    }
}
