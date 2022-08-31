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
public class AccountBalanceDto {

    private Long accountId;

    private Integer accountCode;

    private String accountLabel;

    @JsonSerialize(using = MoneySerializer.class)
    private BigDecimal totalCurrentDebit;

    @JsonSerialize(using = MoneySerializer.class)
    private BigDecimal totalCurrentCredit;

    private boolean isLiterable;

    public AccountBalanceDto(Long accountId, BigDecimal totalCurrentDebit, BigDecimal totalCurrentCredit) {
        super();
        this.accountId = accountId;
        this.totalCurrentDebit = totalCurrentDebit;
        this.totalCurrentCredit = totalCurrentCredit;
    }

}
