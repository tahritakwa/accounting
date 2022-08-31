package fr.sparkit.accounting.dto;

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
public class PaymentAccountDto {

    private Long id;

    private Long taxSalesAccount;

    private Long hTaxSalesAccount;

    private Long taxPurchasesAccount;

    private Long hTaxPurchasesAccount;

    private Long taxId;

}
