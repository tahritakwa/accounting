package fr.sparkit.accounting.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class AccountingConfigurationDto {
    private Long id;
    private Long taxStampIdAccountingAccountSales;
    private Long taxStampIdAccountingAccountPurchase;
    private Long discountIdAccountingAccountSales;
    private Long discountIdAccountingAccountPurchase;
    private Long withHoldingTaxIdAccountingAccount;
    private Long cofferIdAccountingAccount;
    private Long intermediateIdAccountingAccount;
    private Long bankIdAccountingAccount;
    private Long customerAccount;
    private Long supplierAccount;
    private Long taxSalesAccount;
    private Long htaxSalesAccount;
    private Long taxPurchasesAccount;
    private Long htaxPurchasesAccount;
    private Long fodecSalesAccount;
    private Long fodecPurchasesAccount;
    private int withHoldingTaxCode;
    private int cofferIdAccountingAccountCode;
    private int intermediateAccountingAccountCode;
    private int bankIdAccountingAccountCode;
    private int customerAccountCode;
    private int supplierAccountCode;
    private int taxSalesAccountCode;
    private int htaxSalesAccountCode;
    private int taxPurchasesAccountCode;
    private int htaxPurchasesAccountCode;
    private int fodecSalesAccountCode;
    private int fodecPurchasesAccountCode;
    private Long journalANewId;
    private Long journalSalesId;
    private Long journalPurchasesId;
    private Long journalCofferId;
    private Long journalBankId;
    private Long resultAccount;
    private int resultAccountCode;
    private Long tangibleImmobilizationAccount;
    private Long tangibleAmortizationAccount;
    private Long intangibleImmobilizationAccount;
    private Long intangibleAmortizationAccount;
    private Long dotationAmortizationAccount;

}
