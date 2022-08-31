package fr.sparkit.accounting.entities;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "T_ACCOUNTING_CONFIGURATION")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@ToString(exclude = { "isDeleted", "deletedToken" })
public class AccountingConfiguration implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "AC_ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne
    @JoinColumn(name = "AC_TAX_STAMP_ACCOUNTING_ACCOUNT_SALES")
    private Account taxStampAccountingAccountSales;

    @ManyToOne
    @JoinColumn(name = "AC_TAX_STAMP_ACCOUNTING_ACCOUNT_PURCHASE")
    private Account taxStampAccountingAccountPurchase;

    @ManyToOne
    @JoinColumn(name = "AC_DISCOUNT_ACCOUNTING_ACCOUNT_SALES")
    private Account discountAccountingAccountSales;

    @ManyToOne
    @JoinColumn(name = "AC_DISCOUNT_ACCOUNTING_ACCOUNT_PURCHASE")
    private Account discountAccountingAccountPurchase;

    @ManyToOne
    @JoinColumn(name = "AC_WITH_HOLDING_TAX_ACCOUNTING_ACCOUNT")
    private ChartAccounts withHoldingTaxAccountingAccount;

    @ManyToOne
    @JoinColumn(name = "AC_COFFER_ACCOUNTING_ACCOUNT_ID")
    private ChartAccounts cofferAccountingAccount;

    @ManyToOne
    @JoinColumn(name = "AC_INTERMEDIATE_ACCOUNTING_ACCOUNT_ID")
    private ChartAccounts intermediateAccountingAccount;

    @ManyToOne
    @JoinColumn(name = "AC_BANK_ACCOUNTING_ACCOUNT_ID")
    private ChartAccounts bankAccountingAccount;

    @ManyToOne
    @JoinColumn(name = "AC_CUSTOMER_ACCOUNT_ID")
    private ChartAccounts customerAccount;

    @ManyToOne
    @JoinColumn(name = "AC_SUPPLIER_ACCOUNT_ID")
    private ChartAccounts supplierAccount;

    @ManyToOne
    @JoinColumn(name = "AC_TAX_SALES_ACCOUNT_ID")
    private ChartAccounts taxSalesAccount;

    @ManyToOne
    @JoinColumn(name = "AC_H_TAX_SALES_ACCOUNT_ID")
    private ChartAccounts hTaxSalesAccount;

    @ManyToOne
    @JoinColumn(name = "AC_TAX_PURCHASES_ACCOUNT_ID")
    private ChartAccounts taxPurchasesAccount;

    @ManyToOne
    @JoinColumn(name = "AC_H_TAX_PURCHASES_ACCOUNT_ID")
    private ChartAccounts hTaxPurchasesAccount;

    @ManyToOne
    @JoinColumn(name = "AC_FODEC_PURCHASES_ACCOUNT_ID")
    private ChartAccounts fodecPurchasesAccount;

    @ManyToOne
    @JoinColumn(name = "AC_FODEC_SALES_ACCOUNT_ID")
    private ChartAccounts fodecSalesAccount;

    @ManyToOne
    @JoinColumn(name = "AC_JOURNAL_A_NEW_ID")
    private Journal journalANew;

    @ManyToOne
    @JoinColumn(name = "AC_JOURNAL_SALES_ID")
    private Journal journalSales;

    @ManyToOne
    @JoinColumn(name = "AC_JOURNAL_PURCHASES_ID")
    private Journal journalPurchases;

    @ManyToOne
    @JoinColumn(name = "AC_JOURNAL_COFFER_ID")
    private Journal journalCoffer;

    @ManyToOne
    @JoinColumn(name = "AC_JOURNAL_BANK_ID")
    private Journal journalBank;

    @ManyToOne
    @JoinColumn(name = "AC_RESULT_ACCOUNT_ID")
    private ChartAccounts resultAccount;

    @ManyToOne
    @JoinColumn(name = "AC_TANGIBLE_IMMOBILIZATION_ACCOUNT_ID")
    private ChartAccounts tangibleImmobilizationAccount;

    @ManyToOne
    @JoinColumn(name = "AC_TANGIBLE_AMORTIZATION_ACCOUNT_ID")
    private ChartAccounts tangibleAmortizationAccount;

    @ManyToOne
    @JoinColumn(name = "AC_INTANGIBLE_IMMOBILIZATION_ACCOUNT_ID")
    private ChartAccounts intangibleImmobilizationAccount;

    @ManyToOne
    @JoinColumn(name = "AC_INTANGIBLE_AMORTIZATION_ACCOUNT_ID")
    private ChartAccounts intangibleAmortizationAccount;

    @ManyToOne
    @JoinColumn(name = "AC_DOTATION_AMORTIZATION_ACCOUNT_ID")
    private ChartAccounts dotationAmortizationAccount;

    @Column(name = "AC_IS_DELETED", columnDefinition = "bit default 0")
    private boolean isDeleted;

    @Column(name = "AC_DELETED_TOKEN")
    private UUID deletedToken;

    public AccountingConfiguration(Account taxStampAccountingAccountSales, Account taxStampAccountingAccountPurchase,
            Account discountAccountingAccountSales, Account discountAccountingAccountPurchase,
            ChartAccounts withHoldingTaxAccountingAccount, ChartAccounts cofferAccountingAccount,
            ChartAccounts intermediateAccountingAccount, ChartAccounts bankAccountingAccount,
            ChartAccounts customerAccount, ChartAccounts supplierAccount, ChartAccounts taxSalesAccount,
            ChartAccounts hTaxSalesAccount, ChartAccounts taxPurchasesAccount, ChartAccounts hTaxPurchasesAccount,
            ChartAccounts fodecPurchasesAccount, ChartAccounts fodecSalesAccount, Journal journalANew,
            Journal journalSales, Journal journalPurchases, Journal journalCoffer, Journal journalBank,
            ChartAccounts resultAccount, ChartAccounts tangibleImmobilizationAccount,
            ChartAccounts tangibleAmortizationAccount, ChartAccounts intangibleImmobilizationAccount,
            ChartAccounts intangibleAmortizationAccount, ChartAccounts dotationAmortizationAccount) {
        super();
        this.taxStampAccountingAccountSales = taxStampAccountingAccountSales;
        this.taxStampAccountingAccountPurchase = taxStampAccountingAccountPurchase;
        this.discountAccountingAccountSales = discountAccountingAccountSales;
        this.discountAccountingAccountPurchase = discountAccountingAccountPurchase;
        this.withHoldingTaxAccountingAccount = withHoldingTaxAccountingAccount;
        this.cofferAccountingAccount = cofferAccountingAccount;
        this.intermediateAccountingAccount = intermediateAccountingAccount;
        this.bankAccountingAccount = bankAccountingAccount;
        this.customerAccount = customerAccount;
        this.supplierAccount = supplierAccount;
        this.taxSalesAccount = taxSalesAccount;
        this.hTaxSalesAccount = hTaxSalesAccount;
        this.taxPurchasesAccount = taxPurchasesAccount;
        this.hTaxPurchasesAccount = hTaxPurchasesAccount;
        this.fodecPurchasesAccount = fodecPurchasesAccount;
        this.fodecSalesAccount = fodecSalesAccount;
        this.journalANew = journalANew;
        this.journalSales = journalSales;
        this.journalPurchases = journalPurchases;
        this.journalCoffer = journalCoffer;
        this.journalBank = journalBank;
        this.resultAccount = resultAccount;
        this.tangibleImmobilizationAccount = tangibleImmobilizationAccount;
        this.tangibleAmortizationAccount = tangibleAmortizationAccount;
        this.intangibleImmobilizationAccount = intangibleImmobilizationAccount;
        this.intangibleAmortizationAccount = intangibleAmortizationAccount;
        this.dotationAmortizationAccount = dotationAmortizationAccount;
    }

}
