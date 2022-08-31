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
public class TrialBalanceAccountAmountDto {

    // document account lines with status
    // BY_CONCLUDING_CURRENT_FISCAL_YEAR_IS_CREATED will be put in initial amount of
    // trial balance
    // those with the other three status will be put in its current amount
    private int indexOfDocumentAccountStatusToGroupBy;

    @JsonSerialize(using = MoneySerializer.class)
    private BigDecimal totalDebit;

    @JsonSerialize(using = MoneySerializer.class)
    private BigDecimal totalCredit;

}
