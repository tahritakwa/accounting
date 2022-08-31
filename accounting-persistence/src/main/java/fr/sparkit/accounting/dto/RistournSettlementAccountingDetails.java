package fr.sparkit.accounting.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RistournSettlementAccountingDetails {

    private Long idWithHoldingTax;
    private BigDecimal amountWithHoldingTax;
}
