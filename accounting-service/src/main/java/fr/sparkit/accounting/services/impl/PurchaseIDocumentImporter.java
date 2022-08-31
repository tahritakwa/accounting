package fr.sparkit.accounting.services.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import fr.sparkit.accounting.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import fr.sparkit.accounting.constants.NumberConstant;
import fr.sparkit.accounting.convertor.AccountConvertor;
import fr.sparkit.accounting.dao.PaymentAccountDao;
import fr.sparkit.accounting.entities.Account;
import fr.sparkit.accounting.entities.DocumentAccount;
import fr.sparkit.accounting.entities.DocumentAccountLine;
import fr.sparkit.accounting.entities.account.relations.GenericAccountRelation;
import fr.sparkit.accounting.entities.account.relations.PaymentAccount;
import fr.sparkit.accounting.enumuration.DocumentAccountStatus;
import fr.sparkit.accounting.enumuration.VatType;
import fr.sparkit.accounting.services.IAccountService;
import fr.sparkit.accounting.services.IAccountingConfigurationService;
import fr.sparkit.accounting.services.IDocumentAccountService;
import fr.sparkit.accounting.services.IDocumentImporter;
import fr.sparkit.accounting.services.IGenericAccountRelationService;
import fr.sparkit.accounting.services.utils.ImportDocumentUtil;
import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.http.HttpCustomException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PurchaseIDocumentImporter implements IDocumentImporter {

    private final IDocumentAccountService documentAccountService;
    private final IAccountService accountService;
    private final IGenericAccountRelationService accountSupplierService;
    private final IAccountingConfigurationService accountingConfigurationService;
    private final PaymentAccountDao paymentAccountDao;
    private final IGenericAccountRelationService accountWithHoldingTaxService;

    @Autowired
    public PurchaseIDocumentImporter(IDocumentAccountService documentAccountService, IAccountService accountService,
            PaymentAccountDao paymentAccountDao,
            @Qualifier("AccountSupplierService") IGenericAccountRelationService accountSupplierService,
            IAccountingConfigurationService accountingConfigurationService,
            @Qualifier("AccountBankService") IGenericAccountRelationService accountBankService,
            @Qualifier("AccountWithHoldingTaxService") IGenericAccountRelationService accountWithHoldingTaxService) {
        this.documentAccountService = documentAccountService;
        this.accountService = accountService;
        this.paymentAccountDao = paymentAccountDao;
        this.accountSupplierService = accountSupplierService;
        this.accountWithHoldingTaxService = accountWithHoldingTaxService;
        this.accountingConfigurationService = accountingConfigurationService;
    }

    @Override
    public DocumentAccount importDocument(BillDto billDto) {
        List<DocumentAccountLine> documentAccountLines = new ArrayList<>();
        addAmountTTCToClientAccount(billDto, documentAccountLines);
        // adding current sales Tax account
        if (billDto.getTaxStamp().compareTo(BigDecimal.ZERO) > NumberConstant.ZERO) {
            Long purchasesAccountId = accountingConfigurationService.findLastConfig()
                    .getTaxStampIdAccountingAccountPurchase();
            AccountDto purchasesAccountDto = accountService.findById(purchasesAccountId);
            documentAccountLines
                    .add(documentAccountService.getPurchasesTaxStampDocumentLine(billDto, purchasesAccountDto));
        }
        if (billDto.getRistourn().compareTo(BigDecimal.ZERO) > NumberConstant.ZERO) {
            documentAccountLines.add(ImportDocumentUtil.getPurchaseDiscountDocumentLine(billDto, accountService
                    .findById(accountingConfigurationService.findLastConfig().getDiscountIdAccountingAccountSales())));
        }
        if (billDto.getAmountTTC().compareTo(BigDecimal.ZERO) == NumberConstant.ZERO) {
            log.error("Trying to generate a document account from a bill with amount TTC equal to zero");
            throw new HttpCustomException(
                    ApiErrors.Accounting.DOCUMENT_ACCOUNT_FROM_BILL_AMOUNT_TTC_EQUAL_ZERO_CANNOT_BE_GENERATED);
        }
        // Group VatDetails by Sales Account ID and Sum it's total Credit
        // add bill details VAT to our accumulated list
        billDto.getBillDetails().forEach((BillDetailsDto billDetailsDto) -> {
            Optional<PaymentAccount> paymentAccountOpt = paymentAccountDao.findByTaxId(billDetailsDto.getIdTax());
            Account taxAccount;
            Account hTaxAccount;
            if (paymentAccountOpt.isPresent() && paymentAccountOpt.get().getTaxPurchasesAccount() != null
                    && paymentAccountOpt.get().getHTaxPurchasesAccount() != null) {
                taxAccount = accountService.findOne(paymentAccountOpt.get().getTaxPurchasesAccount().getId());
                hTaxAccount = accountService.findOne(paymentAccountOpt.get().getHTaxPurchasesAccount().getId());
            } else {
                if (billDetailsDto.getVatType() == VatType.TVA.getIndex()) {
                    taxAccount = getDocumentAccountLineAccount(ImportDocumentUtil.generateAccountCode(
                            accountingConfigurationService.findLastConfig().getTaxPurchasesAccountCode()));
                } else {
                    taxAccount = getDocumentAccountLineAccount(ImportDocumentUtil.generateAccountCode(
                            accountingConfigurationService.findLastConfig().getFodecPurchasesAccountCode()));
                }
                hTaxAccount = getDocumentAccountLineAccount(ImportDocumentUtil.generateAccountCode(
                        accountingConfigurationService.findLastConfig().getHtaxPurchasesAccountCode()));
            }

            ImportDocumentUtil.balancedDocument(billDto, documentAccountLines, billDetailsDto, taxAccount, hTaxAccount);
        });
        DocumentAccountingDto documentAccountingDto = ImportDocumentUtil.billDetailsDtoToDocumentAccountingDto(billDto,
                documentAccountLines);
        documentAccountingDto.setJournalId(accountingConfigurationService.findLastConfig().getJournalPurchasesId());
        documentAccountingDto.setIndexOfStatus(DocumentAccountStatus.BY_IMPORT_DOCUMENT_IS_CREATED.getIndex());
        documentAccountingDto.setBillId(billDto.getIdDocument());
        return documentAccountService.saveDocumentAccount(documentAccountingDto, false);
    }

    public Account getDocumentAccountLineAccount(StringBuilder accountTaxCode) {
        Optional<Account> accountHTaxOpt = accountService
                .findAccountByCode(Integer.parseInt(accountTaxCode.toString()));
        if (accountHTaxOpt.isPresent()) {
            return accountHTaxOpt.get();
        } else {
            throw new HttpCustomException(ApiErrors.Accounting.ACCOUNT_TAXE_DOES_NOT_EXIST);
        }
    }

    @Override
    public DocumentAccount importRegulation(RegulationDto regulationDto, Long bankAccoutId, Long cofferAccountId, boolean isMultipleRegulationImportation) {
        List<DocumentAccountLine> documentAccountLines = new ArrayList<>();
        Account tierAccount = accountService.findTierAccount(regulationDto.getTierId(), regulationDto.getTiersName(),
                Boolean.TRUE);
        BigDecimal withHoldingTaxSum = BigDecimal.ZERO;
        for (RistournSettlementAccountingDetails ristournSettlementAccountingDetail : regulationDto
                .getRistournSettlementAccountingDetails()) {
            withHoldingTaxSum = withHoldingTaxSum.add(ristournSettlementAccountingDetail.getAmountWithHoldingTax());
        }
        documentAccountLines.add(new DocumentAccountLine(regulationDto.getSettlementDate(),
                regulationDto.getCodeSettlement(), regulationDto.getCodeSettlement(),
                regulationDto.getPaymentAmount().add(withHoldingTaxSum), BigDecimal.ZERO, tierAccount));

        // add total amount Payment for coffer or bank account
        AccountDto accountDto = null;
        Long journalId;
        if (regulationDto.getBankId() == NumberConstant.ZERO) {
            accountDto = accountService.findById(cofferAccountId);
            journalId = accountingConfigurationService.findLastConfig().getJournalCofferId();
        } else {
            accountDto = accountService.findById(bankAccoutId);
            journalId = accountingConfigurationService.findLastConfig().getJournalBankId();
        }

        for (DocumentSettlementAccountingDetails documentSettlementAccountingDetails : regulationDto
                .getDocumentSettlementAccountingDetails()) {
            if (documentSettlementAccountingDetails.getIsAsset()) {
                documentAccountLines.add(new DocumentAccountLine(regulationDto.getSettlementDate(),
                        regulationDto.getCodeSettlement(), regulationDto.getCodeSettlement(),
                        documentSettlementAccountingDetails.getAmountSettlementDocument(), BigDecimal.ZERO,
                        AccountConvertor.dtoToModel(accountDto, null)));
            } else {
                documentAccountLines.add(new DocumentAccountLine(regulationDto.getSettlementDate(),
                        regulationDto.getCodeSettlement(), regulationDto.getCodeSettlement(), BigDecimal.ZERO,
                        documentSettlementAccountingDetails.getAmountSettlementDocument(),
                        AccountConvertor.dtoToModel(accountDto, null)));
            }
        }

        for (RistournSettlementAccountingDetails ristournSettlementAccountingDetail : regulationDto
                .getRistournSettlementAccountingDetails()) {
            Optional<GenericAccountRelation> accountWithHoldingTax = accountWithHoldingTaxService
                    .findAccountWithRelation(ristournSettlementAccountingDetail.getIdWithHoldingTax());
            Account withHoldingTaxAccount;
            if (accountWithHoldingTax.isPresent()) {
                withHoldingTaxAccount = accountWithHoldingTax.get().getAccount();
            } else {
                withHoldingTaxAccount = getDocumentAccountLineAccount(ImportDocumentUtil
                        .generateAccountCode(accountingConfigurationService.findLastConfig().getWithHoldingTaxCode()));
            }
            documentAccountLines.add(new DocumentAccountLine(regulationDto.getSettlementDate(),
                    regulationDto.getCodeSettlement(), regulationDto.getCodeSettlement(), BigDecimal.ZERO,
                    ristournSettlementAccountingDetail.getAmountWithHoldingTax(), withHoldingTaxAccount));
        }
        DocumentAccountingDto documentAccountingDto = ImportDocumentUtil
                .regulationDtoToDocumentAccountingDto(regulationDto, documentAccountLines);
        if(documentAccountService.getFiltredDALines(documentAccountingDto.getDocumentAccountLines()).isEmpty() && isMultipleRegulationImportation){
            return null;
        }
        documentAccountingDto.setJournalId(journalId);
        documentAccountingDto.setIndexOfStatus(DocumentAccountStatus.BY_IMPORT_SETTLEMENT_IS_CREATED.getIndex());
        return documentAccountService.saveDocumentAccount(documentAccountingDto, false);

    }

    @Override
    public DocumentAccount importCreditNote(BillDto billDto) {
        List<DocumentAccountLine> documentAccountLines = new ArrayList<>();
        addAmountTTCToClientAccount(billDto, documentAccountLines);
        // adding current sales Tax account
        if (billDto.getTaxStamp().compareTo(BigDecimal.ZERO) > NumberConstant.ZERO) {
            Long purchasesAccountId = accountingConfigurationService.findLastConfig()
                    .getTaxStampIdAccountingAccountPurchase();
            AccountDto purchasesAccountDto = accountService.findById(purchasesAccountId);
            documentAccountLines.add(documentAccountService.getSalesTaxStampDocumentLine(billDto, purchasesAccountDto));
        }
        if (billDto.getRistourn().compareTo(BigDecimal.ZERO) > NumberConstant.ZERO) {
            documentAccountLines.add(ImportDocumentUtil.getSalesDiscountDocumentLine(billDto, accountService
                    .findById(accountingConfigurationService.findLastConfig().getDiscountIdAccountingAccountSales())));
        }
        // Group VatDetails by Sales Account ID and Sum it's total Credit
        // add bill details VAT to our accumulated list
        billDto.getBillDetails().forEach((BillDetailsDto billDetailsDto) -> {
            Optional<PaymentAccount> paymentAccountOpt = paymentAccountDao.findByTaxId(billDetailsDto.getIdTax());
            Account taxAccount;
            Account hTaxAccount;
            if (paymentAccountOpt.isPresent() && paymentAccountOpt.get().getTaxPurchasesAccount() != null
                    && paymentAccountOpt.get().getHTaxPurchasesAccount() != null) {
                taxAccount = accountService.findOne(paymentAccountOpt.get().getTaxPurchasesAccount().getId());
                hTaxAccount = accountService.findOne(paymentAccountOpt.get().getHTaxPurchasesAccount().getId());
            } else {
                if (billDetailsDto.getVatType() == VatType.TVA.getIndex()) {
                    taxAccount = getDocumentAccountLineAccount(ImportDocumentUtil.generateAccountCode(
                            accountingConfigurationService.findLastConfig().getTaxPurchasesAccountCode()));
                } else {
                    taxAccount = getDocumentAccountLineAccount(ImportDocumentUtil.generateAccountCode(
                            accountingConfigurationService.findLastConfig().getFodecPurchasesAccountCode()));
                }
                hTaxAccount = getDocumentAccountLineAccount(ImportDocumentUtil.generateAccountCode(
                        accountingConfigurationService.findLastConfig().getHtaxPurchasesAccountCode()));
            }

            ImportDocumentUtil.balancedDocument(billDto, documentAccountLines, billDetailsDto, taxAccount, hTaxAccount);
        });
        DocumentAccountingDto documentAccountingDto = ImportDocumentUtil.billDetailsDtoToDocumentAccountingDto(billDto,
                documentAccountLines);
        documentAccountingDto.setJournalId(accountingConfigurationService.findLastConfig().getJournalPurchasesId());
        documentAccountingDto.setIndexOfStatus(DocumentAccountStatus.BY_IMPORT_DOCUMENT_IS_CREATED.getIndex());
        documentAccountingDto.setBillId(billDto.getIdDocument());
        return documentAccountService.saveDocumentAccount(documentAccountingDto, false);
    }

    private void addAmountTTCToClientAccount(BillDto billDto, Collection<DocumentAccountLine> documentAccountLines) {
        Optional<GenericAccountRelation> accountSupplier = accountSupplierService
                .findAccountWithRelation(billDto.getTierId());
        Account tierAccount;
        if (accountSupplier.isPresent()) {
            tierAccount = accountSupplier.get().getAccount();
        } else {
            StringBuilder supplierAccountCode = ImportDocumentUtil
                    .generateAccountCode(accountingConfigurationService.findLastConfig().getSupplierAccountCode());
            Optional<Account> accountTaxOpt = accountService
                    .findAccountByCode(Integer.parseInt(supplierAccountCode.toString()));
            if (accountTaxOpt.isPresent()) {
                tierAccount = accountTaxOpt.get();
            } else {
                throw new HttpCustomException(ApiErrors.Accounting.ACCOUNT_SUPPLIER_DOES_NOT_EXIST);
            }
        }

        switch (billDto.getDocumentType()) {
        case ApiErrors.Accounting.INVOICE_PURCHASES:
            documentAccountLines.add(new DocumentAccountLine(billDto.getDocumentDate(), billDto.getTierName(),
                    billDto.getCodeDocument(), BigDecimal.ZERO, billDto.getAmountTTC(), tierAccount));
            break;
        case ApiErrors.Accounting.PURCHASES_ASSETS:
            documentAccountLines.add(new DocumentAccountLine(billDto.getDocumentDate(), billDto.getTierName(),
                    billDto.getCodeDocument(), billDto.getAmountTTC(), BigDecimal.ZERO, tierAccount));
            break;
        default:
        }
    }

}
