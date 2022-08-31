package fr.sparkit.accounting.services.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import fr.sparkit.accounting.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import fr.sparkit.accounting.constants.AccountingConstants;
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
public class SaleIDocumentImporter implements IDocumentImporter {

    private final IDocumentAccountService documentAccountService;
    private final IAccountService accountService;
    private final IGenericAccountRelationService accountCustomerService;
    private final IAccountingConfigurationService accountingConfigurationService;
    private final PaymentAccountDao paymentAccountDao;
    private final IGenericAccountRelationService accountWithHoldingTaxService;

    @Autowired
    public SaleIDocumentImporter(IDocumentAccountService documentAccountService, IAccountService accountService,
            PaymentAccountDao paymentAccountDao,
            @Qualifier("AccountCustomerService") IGenericAccountRelationService accountCustomerService,
            @Qualifier("AccountWithHoldingTaxService") IGenericAccountRelationService accountWithHoldingTaxService,
            IAccountingConfigurationService accountingConfigurationService) {
        this.documentAccountService = documentAccountService;
        this.accountService = accountService;
        this.paymentAccountDao = paymentAccountDao;
        this.accountCustomerService = accountCustomerService;
        this.accountWithHoldingTaxService = accountWithHoldingTaxService;
        this.accountingConfigurationService = accountingConfigurationService;
    }

    @Override
    public DocumentAccount importDocument(BillDto billDto) {
        List<DocumentAccountLine> documentAccountLines = new ArrayList<>();
        addAmountTTCToClientAccount(billDto, documentAccountLines);
        if (billDto.getTaxStamp().compareTo(BigDecimal.ZERO) > NumberConstant.ZERO) {
            AccountDto salesAccountDto = accountService
                    .findById(accountingConfigurationService.findLastConfig().getTaxStampIdAccountingAccountSales());
            documentAccountLines.add(documentAccountService.getSalesTaxStampDocumentLine(billDto, salesAccountDto));
        }
        if (billDto.getRistourn().compareTo(BigDecimal.ZERO) > NumberConstant.ZERO) {
            documentAccountLines.add(ImportDocumentUtil.getSalesDiscountDocumentLine(billDto, accountService
                    .findById(accountingConfigurationService.findLastConfig().getDiscountIdAccountingAccountSales())));
        }
        if (billDto.getAmountTTC().compareTo(BigDecimal.ZERO) == NumberConstant.ZERO) {
            log.error("Trying to generate a document account from a bill with amount TTC equal to zero");
            throw new HttpCustomException(
                    ApiErrors.Accounting.DOCUMENT_ACCOUNT_FROM_BILL_AMOUNT_TTC_EQUAL_ZERO_CANNOT_BE_GENERATED);
        }
        billDto.getBillDetails().forEach((BillDetailsDto billDetailsDto) -> {
            Optional<PaymentAccount> paymentAccountOpt = paymentAccountDao.findByTaxId(billDetailsDto.getIdTax());
            Account taxAccount;
            Account hTaxAccount;
            if (paymentAccountOpt.isPresent() && paymentAccountOpt.get().getTaxSalesAccount() != null
                    && paymentAccountOpt.get().getHTaxSalesAccount() != null) {
                taxAccount = accountService.findOne(paymentAccountOpt.get().getTaxSalesAccount().getId());
                hTaxAccount = accountService.findOne(paymentAccountOpt.get().getHTaxSalesAccount().getId());
            } else {
                if (billDetailsDto.getVatType() == VatType.TVA.getIndex()) {
                    taxAccount = getDocumentAccountLineAccount(ImportDocumentUtil.generateAccountCode(
                            accountingConfigurationService.findLastConfig().getTaxSalesAccountCode()));
                } else {
                    taxAccount = getDocumentAccountLineAccount(ImportDocumentUtil.generateAccountCode(
                            accountingConfigurationService.findLastConfig().getFodecSalesAccountCode()));
                }
                hTaxAccount = getDocumentAccountLineAccount(ImportDocumentUtil.generateAccountCode(
                        accountingConfigurationService.findLastConfig().getHtaxSalesAccountCode()));
            }
            ImportDocumentUtil.balancedDocument(billDto, documentAccountLines, billDetailsDto, taxAccount, hTaxAccount);

        });

        DocumentAccountingDto documentAccountingDto = ImportDocumentUtil.billDetailsDtoToDocumentAccountingDto(billDto,
                documentAccountLines);
        documentAccountingDto.setJournalId(accountingConfigurationService.findLastConfig().getJournalSalesId());
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
                Boolean.FALSE);
        BigDecimal withHoldingTaxSum = BigDecimal.ZERO;
        for (RistournSettlementAccountingDetails ristournSettlementAccountingDetail : regulationDto
                .getRistournSettlementAccountingDetails()) {
            withHoldingTaxSum = withHoldingTaxSum.add(ristournSettlementAccountingDetail.getAmountWithHoldingTax());
        }
        documentAccountLines.add(new DocumentAccountLine(regulationDto.getSettlementDate(),
                regulationDto.getCodeSettlement(), regulationDto.getCodeSettlement(), BigDecimal.ZERO,
                regulationDto.getPaymentAmount().add(withHoldingTaxSum), tierAccount));

        AccountingConfigurationDto accountingConfigurationDto = accountingConfigurationService.findLastConfig();
        Long journalId;

        AccountDto accountDto;
        if (regulationDto.getBankName() == null || regulationDto.getBankName().equals(AccountingConstants.EMPTY_STRING)) {
            accountDto = accountService.findById(cofferAccountId);
            journalId = accountingConfigurationDto.getJournalCofferId();
        } else {
            accountDto = accountService.findById(bankAccoutId);
            journalId = accountingConfigurationDto.getJournalBankId();
        }
        for (DocumentSettlementAccountingDetails documentSettlementAccountingDetails : regulationDto
                .getDocumentSettlementAccountingDetails()) {
            if (documentSettlementAccountingDetails.getIsAsset()) {
                documentAccountLines.add(new DocumentAccountLine(regulationDto.getSettlementDate(),
                        regulationDto.getCodeSettlement(), documentSettlementAccountingDetails.getCodeDocument(),
                        BigDecimal.ZERO, documentSettlementAccountingDetails.getAmountSettlementDocument(),
                        AccountConvertor.dtoToModel(accountDto, null)));
            } else {
                documentAccountLines.add(new DocumentAccountLine(regulationDto.getSettlementDate(),
                        regulationDto.getCodeSettlement(), documentSettlementAccountingDetails.getCodeDocument(),
                        documentSettlementAccountingDetails.getAmountSettlementDocument(), BigDecimal.ZERO,
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
                    regulationDto.getCodeSettlement(), regulationDto.getCodeSettlement(),
                    ristournSettlementAccountingDetail.getAmountWithHoldingTax(), BigDecimal.ZERO,
                    withHoldingTaxAccount));
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
        if (billDto.getTaxStamp().compareTo(BigDecimal.ZERO) > 0) {
            Long salesAccountId = accountingConfigurationService.findLastConfig().getTaxStampIdAccountingAccountSales();
            AccountDto salesAccountDto = accountService.findById(salesAccountId);
            documentAccountLines.add(documentAccountService.getPurchasesTaxStampDocumentLine(billDto, salesAccountDto));
        }
        if (billDto.getRistourn().compareTo(BigDecimal.ZERO) > NumberConstant.ZERO) {
            documentAccountLines.add(ImportDocumentUtil.getPurchaseDiscountDocumentLine(billDto, accountService
                    .findById(accountingConfigurationService.findLastConfig().getDiscountIdAccountingAccountSales())));
        }
        billDto.getBillDetails().forEach((BillDetailsDto billDetailsDto) -> {
            Optional<PaymentAccount> paymentAccountOpt = paymentAccountDao.findByTaxId(billDetailsDto.getIdTax());
            Account taxAccount;
            Account hTaxAccount;
            if (paymentAccountOpt.isPresent() && paymentAccountOpt.get().getTaxSalesAccount() != null
                    && paymentAccountOpt.get().getHTaxSalesAccount() != null) {
                taxAccount = accountService.findOne(paymentAccountOpt.get().getTaxSalesAccount().getId());
                hTaxAccount = accountService.findOne(paymentAccountOpt.get().getHTaxSalesAccount().getId());
            } else {
                if (billDetailsDto.getVatType() == VatType.TVA.getIndex()) {
                    taxAccount = getDocumentAccountLineAccount(ImportDocumentUtil.generateAccountCode(
                            accountingConfigurationService.findLastConfig().getTaxSalesAccountCode()));
                } else {
                    taxAccount = getDocumentAccountLineAccount(ImportDocumentUtil.generateAccountCode(
                            accountingConfigurationService.findLastConfig().getFodecSalesAccountCode()));
                }
                hTaxAccount = getDocumentAccountLineAccount(ImportDocumentUtil.generateAccountCode(
                        accountingConfigurationService.findLastConfig().getHtaxSalesAccountCode()));
            }

            ImportDocumentUtil.balancedDocument(billDto, documentAccountLines, billDetailsDto, taxAccount, hTaxAccount);
        });
        DocumentAccountingDto documentAccountingDto = ImportDocumentUtil.billDetailsDtoToDocumentAccountingDto(billDto,
                documentAccountLines);
        documentAccountingDto.setJournalId(accountingConfigurationService.findLastConfig().getJournalSalesId());
        documentAccountingDto.setIndexOfStatus(DocumentAccountStatus.BY_IMPORT_DOCUMENT_IS_CREATED.getIndex());
        documentAccountingDto.setBillId(billDto.getIdDocument());
        return documentAccountService.saveDocumentAccount(documentAccountingDto, false);
    }

    private void addAmountTTCToClientAccount(BillDto billDto, List<DocumentAccountLine> documentAccountLines) {
        Optional<GenericAccountRelation> accountCustomer = accountCustomerService
                .findAccountWithRelation(billDto.getTierId());
        Account tierAccount;
        if (accountCustomer.isPresent()) {
            tierAccount = accountCustomer.get().getAccount();
        } else {
            StringBuilder customerAccountCode = ImportDocumentUtil
                    .generateAccountCode(accountingConfigurationService.findLastConfig().getCustomerAccountCode());
            Optional<Account> accountTaxOpt = accountService
                    .findAccountByCode(Integer.parseInt(customerAccountCode.toString()));
            if (accountTaxOpt.isPresent()) {
                tierAccount = accountTaxOpt.get();
            } else {
                throw new HttpCustomException(ApiErrors.Accounting.ACCOUNT_CUSTOMER_DOES_NOT_EXIST);
            }
        }
        switch (billDto.getDocumentType()) {
        case ApiErrors.Accounting.INVOICE_SALES:
            documentAccountLines.add(new DocumentAccountLine(billDto.getDocumentDate(), billDto.getTierName(),
                    billDto.getCodeDocument(), billDto.getAmountTTC(), BigDecimal.ZERO, tierAccount));
            break;
        case ApiErrors.Accounting.SALES_ASSETS:
            documentAccountLines.add(new DocumentAccountLine(billDto.getDocumentDate(), billDto.getTierName(),
                    billDto.getCodeDocument(), BigDecimal.ZERO, billDto.getAmountTTC(), tierAccount));
            break;
        default:
        }
    }




}
