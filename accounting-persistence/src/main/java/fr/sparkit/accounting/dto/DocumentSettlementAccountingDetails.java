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
public class DocumentSettlementAccountingDetails {
    private String codeDocument;
    private BigDecimal amountSettlementDocument;
    private Boolean isAsset;
}
