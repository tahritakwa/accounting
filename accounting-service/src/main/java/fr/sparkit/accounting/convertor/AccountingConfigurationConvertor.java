package fr.sparkit.accounting.convertor;

import fr.sparkit.accounting.dto.AccountingConfigurationDto;
import fr.sparkit.accounting.entities.Account;
import fr.sparkit.accounting.entities.AccountingConfiguration;
import fr.sparkit.accounting.entities.ChartAccounts;
import fr.sparkit.accounting.entities.Journal;

public final class AccountingConfigurationConvertor {
    private AccountingConfigurationConvertor() {
        super();
    }

    public static AccountingConfigurationDto modelToDto(AccountingConfiguration accountingConfiguration) {
        if (accountingConfiguration == null) {
            return null;
        }
        return new AccountingConfigurationDto(accountingConfiguration.getId(),
                accountingConfiguration.getTaxStampAccountingAccountSales().getId(),
                accountingConfiguration.getTaxStampAccountingAccountPurchase().getId(),
                accountingConfiguration.getDiscountAccountingAccountSales().getId(),
                accountingConfiguration.getDiscountAccountingAccountPurchase().getId(),
                accountingConfiguration.getWithHoldingTaxAccountingAccount().getId(),
                accountingConfiguration.getCofferAccountingAccount().getId(),
                accountingConfiguration.getIntermediateAccountingAccount().getId(),
                accountingConfiguration.getBankAccountingAccount().getId(),
                accountingConfiguration.getCustomerAccount().getId(),
                accountingConfiguration.getSupplierAccount().getId(),
                accountingConfiguration.getTaxSalesAccount().getId(),
                accountingConfiguration.getHTaxSalesAccount().getId(),
                accountingConfiguration.getTaxPurchasesAccount().getId(),
                accountingConfiguration.getHTaxPurchasesAccount().getId(),
                accountingConfiguration.getFodecPurchasesAccount().getId(),
                accountingConfiguration.getFodecSalesAccount().getId(),
                accountingConfiguration.getWithHoldingTaxAccountingAccount().getCode(),
                accountingConfiguration.getCofferAccountingAccount().getCode(),
                accountingConfiguration.getIntermediateAccountingAccount().getCode(),
                accountingConfiguration.getBankAccountingAccount().getCode(),
                accountingConfiguration.getCustomerAccount().getCode(),
                accountingConfiguration.getSupplierAccount().getCode(),
                accountingConfiguration.getTaxSalesAccount().getCode(),
                accountingConfiguration.getHTaxSalesAccount().getCode(),
                accountingConfiguration.getTaxPurchasesAccount().getCode(),
                accountingConfiguration.getHTaxPurchasesAccount().getCode(),
                accountingConfiguration.getFodecSalesAccount().getCode(),
                accountingConfiguration.getFodecPurchasesAccount().getCode(),
                accountingConfiguration.getJournalANew().getId(), accountingConfiguration.getJournalSales().getId(),
                accountingConfiguration.getJournalPurchases().getId(),
                accountingConfiguration.getJournalCoffer().getId(), accountingConfiguration.getJournalBank().getId(),
                accountingConfiguration.getResultAccount().getId(),
                accountingConfiguration.getResultAccount().getCode(),
                accountingConfiguration.getTangibleImmobilizationAccount().getId(),
                accountingConfiguration.getTangibleAmortizationAccount().getId(),
                accountingConfiguration.getIntangibleImmobilizationAccount().getId(),
                accountingConfiguration.getIntangibleAmortizationAccount().getId(),
                accountingConfiguration.getDotationAmortizationAccount().getId());
    }

    public static AccountingConfiguration dtoToModel(Account taxStampIdAccountingAccountSales,
            Account taxStampIdAccountingAccountPurchase, Account discountIdAccountingAccountSales,
            Account discountIdAccountingAccountPurchase, ChartAccounts withHoldingTaxAccountingAccount,
            ChartAccounts cofferIdAccountingAccount, ChartAccounts intermediateIdAccountingAccount,
            ChartAccounts bankIdAccountingAccount, ChartAccounts customerAccount, ChartAccounts supplierAccount,
            ChartAccounts taxSalesAccount, ChartAccounts hTaxSalesAccount, ChartAccounts taxPurchasesAccount,
            ChartAccounts hTaxPurchasesAccount, ChartAccounts fodecSalesAccount, ChartAccounts fodecPurchasesAccount,
            Journal journalANew, Journal journalSales, Journal journalPurchases, Journal journalCoffer,
            Journal journalBank, ChartAccounts resultAccount, ChartAccounts tangibleImmobilizationAccount,
            ChartAccounts tangibleAmortizationAccount, ChartAccounts intangibleImmobilizationAccount,
            ChartAccounts intangibleAmortizationAccount, ChartAccounts dotationAmortizationAccount) {
        return new AccountingConfiguration(taxStampIdAccountingAccountSales, taxStampIdAccountingAccountPurchase,
                discountIdAccountingAccountSales, discountIdAccountingAccountPurchase, withHoldingTaxAccountingAccount,
                cofferIdAccountingAccount, intermediateIdAccountingAccount, bankIdAccountingAccount, customerAccount,
                supplierAccount, taxSalesAccount, hTaxSalesAccount, taxPurchasesAccount, hTaxPurchasesAccount,
                fodecPurchasesAccount, fodecSalesAccount, journalANew, journalSales, journalPurchases, journalCoffer,
                journalBank, resultAccount, tangibleImmobilizationAccount, tangibleAmortizationAccount,
                intangibleImmobilizationAccount, intangibleAmortizationAccount, dotationAmortizationAccount);
    }
}
