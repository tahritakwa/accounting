package fr.sparkit.accounting.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import fr.sparkit.accounting.auditing.MoneySerializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TrialBalanceAccountDto {

    private AccountDto accountDto;

    @JsonSerialize(using = MoneySerializer.class)
    private BigDecimal totalInitialDebit;

    @JsonSerialize(using = MoneySerializer.class)
    private BigDecimal totalInitialCredit;

    @JsonSerialize(using = MoneySerializer.class)
    private BigDecimal totalCurrentDebit;

    @JsonSerialize(using = MoneySerializer.class)
    private BigDecimal totalCurrentCredit;

    @JsonSerialize(using = MoneySerializer.class)
    private BigDecimal accumulatedDebit;

    @JsonSerialize(using = MoneySerializer.class)
    private BigDecimal accumulatedCredit;

    @JsonSerialize(using = MoneySerializer.class)
    private BigDecimal balanceDebit;

    @JsonSerialize(using = MoneySerializer.class)
    private BigDecimal balanceCredit;

    public TrialBalanceAccountDto() {
        super();
        this.totalInitialDebit = BigDecimal.ZERO;
        this.totalInitialCredit = BigDecimal.ZERO;
        this.totalCurrentDebit = BigDecimal.ZERO;
        this.totalCurrentCredit = BigDecimal.ZERO;
        this.accumulatedDebit = BigDecimal.ZERO;
        this.accumulatedCredit = BigDecimal.ZERO;
        this.balanceDebit = BigDecimal.ZERO;
        this.balanceCredit = BigDecimal.ZERO;
    }

}
