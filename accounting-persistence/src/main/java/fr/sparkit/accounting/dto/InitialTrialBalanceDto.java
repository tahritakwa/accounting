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
public class InitialTrialBalanceDto {

    private Long accountId;

    @JsonSerialize(using = MoneySerializer.class)
    private BigDecimal totalInitialDebit;

    @JsonSerialize(using = MoneySerializer.class)
    private BigDecimal totalInitialCredit;

}
