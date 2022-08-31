package fr.sparkit.accounting.services.impl;

import fr.sparkit.accounting.constants.AccountingConstants;
import fr.sparkit.accounting.constants.NumberConstant;
import fr.sparkit.accounting.dao.AccountDao;
import fr.sparkit.accounting.dao.BillDocumentDao;
import fr.sparkit.accounting.dao.PaymentAccountDao;
import fr.sparkit.accounting.dto.*;
import fr.sparkit.accounting.entities.Account;
import fr.sparkit.accounting.entities.BillDocument;
import fr.sparkit.accounting.entities.DocumentAccount;
import fr.sparkit.accounting.entities.account.relations.PaymentAccount;
import fr.sparkit.accounting.services.*;
import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.errors.ErrorsResponse;
import fr.sparkit.accounting.util.http.HttpCustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BillDocumentService extends GenericService<BillDocument, Long> implements IBillDocumentService {

        private static final String PURCHASE_I_DOCUMENT_IMPORTER = "purchaseIDocumentImporter";
        private static final String SALE_I_DOCUMENT_IMPORTER = "saleIDocumentImporter";

        private final IFiscalYearService fiscalYearService;
        private final IDocumentAccountService documentAccountService;
        private final IAccountService accountService;
        private final AccountDao accountDao;
        private final BillDocumentDao billDocumentDao;
        private final PaymentAccountDao paymentAccountDao;
        private final Map<String, IDocumentImporter> documentImporterMap;

        @Value("${dotnet.url}") private String dotnetRessource;

        @Autowired
        public BillDocumentService(IFiscalYearService fiscalYearService,
                Map<String, IDocumentImporter> documentImporterMap, IDocumentAccountService documentAccountService,
                IAccountService accountService, AccountDao accountDao, BillDocumentDao billDocumentDao,
                PaymentAccountDao paymentAccountDao) {
                super();
                this.billDocumentDao = billDocumentDao;
                this.fiscalYearService = fiscalYearService;
                this.documentImporterMap = documentImporterMap;
                this.documentAccountService = documentAccountService;
                this.paymentAccountDao = paymentAccountDao;
                this.accountService = accountService;
                this.accountDao = accountDao;
        }

        @Override
        public BillSuccessDto importBill(BillDto billDto, String contentType, String user, String authorization,
                Long fiscalYearId, List<BillDto> billFailedDtos, List<String> listBillImported,
                List<Integer> httpErrorCodes, List<LocalDateTime> billIdNotInCurrentFiscalYear) {
                RestTemplate restTemplate = new RestTemplate();
                IDocumentImporter documentImporter;
                DocumentAccount documentAccount = null;
                if (verifyAndAddDocumentNotImported(billDto.getIdDocument(), billDto.getCodeDocument())) {
                        listBillImported.add(billDto.getCodeDocument());
                        return null;
                }
                if (verifyBillDateInCurrentFiscalYear(billDto.getDocumentDate(), fiscalYearId)) {
                        billIdNotInCurrentFiscalYear.add(billDto.getDocumentDate());
                        return null;
                }
                try {
                        switch (billDto.getDocumentType()) {
                        case "I-SA":
                                documentImporter = documentImporterMap.get(SALE_I_DOCUMENT_IMPORTER);
                                documentAccount = documentImporter.importDocument(billDto);
                                break;
                        case "I-PU":
                                documentImporter = documentImporterMap.get(PURCHASE_I_DOCUMENT_IMPORTER);
                                documentAccount = documentImporter.importDocument(billDto);
                                break;
                        case "IA-SA":
                                documentImporter = documentImporterMap.get(SALE_I_DOCUMENT_IMPORTER);
                                documentAccount = documentImporter.importCreditNote(billDto);
                                break;
                        case "A-PU":
                                documentImporter = documentImporterMap.get(PURCHASE_I_DOCUMENT_IMPORTER);
                                documentAccount = documentImporter.importCreditNote(billDto);
                                break;
                        default:
                        }
                } catch (HttpCustomException ex) {
                        httpErrorCodes.add(ex.getErrorCode());
                } catch (Exception e) {
                        log.error(AccountingConstants.TRYING_TO_SAVE_DA_IN_CLOSED_PERIOD);
                        billFailedDtos.add(billDto);
                }

                if (documentAccount != null) {
                        BillDocument savedBillDocument = saveAndFlush(
                                new BillDocument(billDto.getIdDocument(), documentAccount, LocalDateTime.now(),
                                        billDto.getDocumentType()));
                        log.info(AccountingConstants.LOG_ENTITY_CREATED, savedBillDocument);
                        accountedInvoiceInDotnet(contentType, user, authorization, restTemplate, savedBillDocument);
                        return new BillSuccessDto(savedBillDocument.getIdBill(), documentAccount.getId());
                } else {
                        log.error(AccountingConstants.DOCUMENT_ACCOUNT_NULL);
                        return null;
                }
        }

        private boolean verifyAndAddDocumentNotImported(Long id, String code) {
                if (billDocumentDao.existsByIdBillAndIsDeletedFalse(id)) {
                        log.error(AccountingConstants.DOCUMENT_WITH_CODE_ALREADY_IMPORTED, code);
                        return true;
                }
                return false;
        }

        private void accountedInvoiceInDotnet(String contentType, String user, String authorization,
                RestTemplate restTemplate, BillDocument savedBillDocument) {
                String fooResourceUrl = dotnetRessource.concat(AccountingConstants.GET_ACCOUNTED_DOCUMENT_URL_DOTNET);
                HttpHeaders headers = new HttpHeaders();
                headers.set("Module", "Sales");
                headers.set("TModel", "Document");
                headers.set("User", user);
                headers.set("Content-Type", contentType);
                headers.set("Authorization", authorization);
                try {
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        HttpEntity<Long> entity = new HttpEntity<>(savedBillDocument.getIdBill(), headers);
                        restTemplate.exchange(fooResourceUrl, HttpMethod.POST, entity, Object.class);
                } catch (RestClientException e) {
                        log.error(AccountingConstants.CANNOT_CONNECT_TO_DOT_NET, e);
                        throw new HttpCustomException(ApiErrors.Accounting.RESOURCE_NOT_FOUND);
                }
        }

        @Override
        public ImportMultipleBillDto importMultipleBill(DocumentsToImportDto documentsToImport, String contentType,
                String user, String authorization) {
                List<BillSuccessDto> billSuccessDtos = new ArrayList<>();
                List<BillDto> billFailedDtos = new ArrayList<>();
                List<String> listBillImported = new ArrayList<>();
                List<Integer> httpErrorCodes = new ArrayList<>();
                List<LocalDateTime> billIdNotInCurrentFiscalYear = new ArrayList<>();
                documentsToImport.getBillDtos().stream().filter(Objects::nonNull).forEach((BillDto bill) -> {
                        BillSuccessDto billSuccess = this
                                .importBill(bill, contentType, user, authorization, documentsToImport.getFiscalYearId(),
                                        billFailedDtos, listBillImported, httpErrorCodes, billIdNotInCurrentFiscalYear);
                        if (billSuccess != null) {
                                billSuccessDtos.add(billSuccess);
                        }
                });
                return new ImportMultipleBillDto(billSuccessDtos, billIdNotInCurrentFiscalYear, listBillImported,
                        billFailedDtos, httpErrorCodes);
        }

        private void verifyDocumentNotImported(Long id, String code) {
                if (billDocumentDao.existsByIdBillAndIsDeletedFalse(id)) {
                        log.error(AccountingConstants.DOCUMENT_WITH_CODE_ALREADY_IMPORTED, code);
                        throw new HttpCustomException(ApiErrors.Accounting.BILL_ALREADY_IMPORTED,
                                new ErrorsResponse().error(code));
                }
        }

        @Override
        public boolean existsByBillId(Long idBill) {
                return billDocumentDao.existsByIdBillAndIsDeletedFalse(idBill);
        }

        public boolean verifyBillDateInCurrentFiscalYear(LocalDateTime date, Long fiscalYearId) {
                FiscalYearDto fiscalYearDto = fiscalYearService.findById(fiscalYearId);
                Long fiscalYearOfDateId = fiscalYearService.findFiscalYearOfDate(date);
                if (fiscalYearOfDateId != null && !fiscalYearOfDateId.equals(fiscalYearDto.getId())) {
                        log.error(AccountingConstants.DATE_DOCUMENT_NOT_CORRESPOND_TO_CURRENT_FISCAL_YEAR);
                        return true;
                }
                return false;
        }

        @Override
        public BillSuccessDto deleteBill(Long documentId) {
                Optional<BillDocument> billDocument = billDocumentDao
                        .findByDocumentAccountIdAndIsDeletedFalse(documentId);
                if (billDocument.isPresent()) {
                        DocumentAccount documentAccountToBeDeleted = documentAccountService
                                .checkIfCanDelete(documentId);
                        delete(billDocument.get().getId());
                        documentAccountService.deleteDocument(documentId, documentAccountToBeDeleted);
                        log.info(AccountingConstants.LOG_ENTITY_DELETED, AccountingConstants.ENTITY_NAME_BILL_DOCUMENT,
                                billDocument.get().getId());
                        return new BillSuccessDto(billDocument.get().getIdBill(), documentId);
                } else {
                        log.error(AccountingConstants.NO_EXIST_BILL_DOCUMENT, documentId);
                        throw new HttpCustomException(ApiErrors.Accounting.NULL_BILL_ID);
                }

        }

        @Override
        public List<DocumentAccount> findDocuments(List<Long> documentIds) {
                List<DocumentAccount> documentsList = new ArrayList<>();
                documentIds.stream().collect(Collectors.toList()).forEach((Long documentId) -> {
                        Optional<BillDocument> billDocument = billDocumentDao.findByIdBillAndIsDeletedFalse(documentId);
                        if (billDocument.isPresent()) {
                                documentsList.add(billDocument.get().getDocumentAccount());
                        } else {
                                documentsList.add(new DocumentAccount());
                        }

                });
                return documentsList;
        }

        @Override
        public List<BillSuccessDto> importMultipleRegulation(SettlementsToImportDto settlementsToImportDtos,
                String contentType, String user, String authorization) {
                List<BillSuccessDto> billSuccessDtos = new ArrayList<>();
                if (settlementsToImportDtos.getRegulationsDtos().size()> NumberConstant.ONE){
                        settlementsToImportDtos.getRegulationsDtos().forEach((RegulationDto regulationDto) -> billSuccessDtos
                                .add(this.importRegulation(regulationDto, settlementsToImportDtos.getBankAccountId(),
                                        settlementsToImportDtos.getCofferAccountId(), contentType, user, authorization, true)));
                } else {
                        billSuccessDtos
                                .add(this.importRegulation(settlementsToImportDtos.getRegulationsDtos().get(0), settlementsToImportDtos.getBankAccountId(),
                                        settlementsToImportDtos.getCofferAccountId(), contentType, user, authorization, false));
                }
                return billSuccessDtos;
        }

        public BillSuccessDto importRegulation(RegulationDto regulationDto, Long bankAccoutId, Long cofferAccountId,
                String contentType, String user, String authorization, boolean isMultipleRegulationImportation) {
                RestTemplate restTemplate = new RestTemplate();
                Objects.requireNonNull(regulationDto, "RegulationDto is null");
                verifyDocumentNotImported(regulationDto.getIdSettlement(), regulationDto.getCodeSettlement());
                IDocumentImporter documentImporter;
                DocumentAccount documentAccount = new DocumentAccount();
                switch (regulationDto.getDocumentType()) {
                case "S-SA":
                        documentImporter = documentImporterMap.get(SALE_I_DOCUMENT_IMPORTER);
                        documentAccount = documentImporter
                                .importRegulation(regulationDto, bankAccoutId, cofferAccountId, isMultipleRegulationImportation);
                        break;
                case "S-PU":
                        documentImporter = documentImporterMap.get(PURCHASE_I_DOCUMENT_IMPORTER);
                        documentAccount = documentImporter
                                .importRegulation(regulationDto, bankAccoutId, cofferAccountId, isMultipleRegulationImportation);
                        break;
                default:
                }

                if (documentAccount != null) {
                        BillDocument savedSettlement = saveAndFlush(
                                new BillDocument(regulationDto.getIdSettlement(), documentAccount, LocalDateTime.now(),
                                        regulationDto.getDocumentType()));
                        log.info(AccountingConstants.LOG_ENTITY_CREATED, savedSettlement);
                        changeSettlementStatus(contentType, user, authorization, restTemplate, savedSettlement);
                        return new BillSuccessDto(savedSettlement.getId(), documentAccount.getId());
                } else {
                        return new BillSuccessDto(null, null);
                }
        }

        private void changeSettlementStatus(String contentType, String user, String authorization,
                RestTemplate restTemplate, BillDocument savedSettlement) {
                String fooResourceUrl = dotnetRessource.concat(AccountingConstants.ACCOUNTED_SETTLEMENT_URL_DOTNET);
                HttpHeaders headers = new HttpHeaders();
                headers.set("Module", "Settlement");
                headers.set("TModel", "Settlement");
                headers.set("User", user);
                headers.set("Content-Type", contentType);
                headers.set("Authorization", authorization);
                try {
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        HttpEntity<Long> entity = new HttpEntity<>(savedSettlement.getIdBill(), headers);
                        restTemplate.postForEntity(fooResourceUrl, entity, Object.class);
                } catch (RestClientException e) {
                        log.error(AccountingConstants.CANNOT_CONNECT_TO_DOT_NET, e);
                        throw new HttpCustomException(ApiErrors.Accounting.RESOURCE_NOT_FOUND);
                }
        }

        @Override
        public PaymentAccount importTaxs(PaymentAccountDto paymentAccountDto) {
                Optional<PaymentAccount> paymentAccount = paymentAccountDao.findByTaxId(paymentAccountDto.getTaxId());
                if (paymentAccount.isPresent()) {
                        Account taxeSalesAccount = accountDao.findOne(paymentAccountDto.getTaxSalesAccount());
                        Account taxePurshasesAccount = accountDao.findOne(paymentAccountDto.getTaxPurchasesAccount());
                        Account hTaxSalesAccount = accountDao.findOne(paymentAccountDto.getHTaxSalesAccount());
                        Account hTaxPurchasesAccount = accountDao.findOne(paymentAccountDto.getHTaxPurchasesAccount());

                        paymentAccount.get().setTaxSalesAccount(taxeSalesAccount);
                        paymentAccount.get().setTaxPurchasesAccount(taxePurshasesAccount);
                        paymentAccount.get().setHTaxSalesAccount(hTaxSalesAccount);
                        paymentAccount.get().setHTaxPurchasesAccount(hTaxPurchasesAccount);
                        paymentAccount.get().setTaxId(paymentAccountDto.getTaxId());
                        paymentAccount.get().setDeleted(false);
                        paymentAccount.get().setDeletedToken(null);
                        return paymentAccountDao.saveAndFlush(paymentAccount.get());
                } else {
                        return paymentAccountDao.saveAndFlush(new PaymentAccount(paymentAccountDto.getId(),
                                accountService.findOne(paymentAccountDto.getTaxSalesAccount()),
                                accountService.findOne(paymentAccountDto.getHTaxSalesAccount()),
                                accountService.findOne(paymentAccountDto.getTaxPurchasesAccount()),
                                accountService.findOne(paymentAccountDto.getHTaxPurchasesAccount()),
                                paymentAccountDto.getTaxId(), false, null));
                }
        }

        @Override
        public PaymentAccountDto findPaymentAccountsByTaxId(Long taxId) {

                Optional<PaymentAccount> paymentAccountOpt = paymentAccountDao.findByTaxId(taxId);
                if (paymentAccountOpt.isPresent()) {
                        Long taxSalesAccountId = null;
                        Long hTaxSalesAccount = null;
                        Long taxPurshasesAccount = null;
                        Long hTaxPurshasesAccount = null;

                        if (paymentAccountOpt.get().getTaxSalesAccount() != null) {
                                taxSalesAccountId = paymentAccountOpt.get().getTaxSalesAccount().getId();
                        }
                        if (paymentAccountOpt.get().getHTaxSalesAccount() != null) {
                                hTaxSalesAccount = paymentAccountOpt.get().getHTaxSalesAccount().getId();
                        }
                        if (paymentAccountOpt.get().getTaxPurchasesAccount() != null) {
                                taxPurshasesAccount = paymentAccountOpt.get().getTaxPurchasesAccount().getId();
                        }
                        if (paymentAccountOpt.get().getHTaxPurchasesAccount() != null) {
                                hTaxPurshasesAccount = paymentAccountOpt.get().getHTaxPurchasesAccount().getId();
                        }
                        return new PaymentAccountDto(paymentAccountOpt.get().getId(), taxSalesAccountId,
                                hTaxSalesAccount, taxPurshasesAccount, hTaxPurshasesAccount, taxId);
                } else {
                        return new PaymentAccountDto();
                }
        }

        @Override
        public Optional<BillDocument> findByDocumentAccountId(Long idDocument) {
                return billDocumentDao.findByDocumentAccountIdAndIsDeletedFalse(idDocument);
        }

        @Override
        public Boolean replaceSettlements(Long settlementId) {
                Optional<BillDocument> billDocumentOpt = billDocumentDao.findByIdBillAndIsDeletedFalse(settlementId);
                if (billDocumentOpt.isPresent()) {
                        documentAccountService.delete(billDocumentOpt.get().getDocumentAccount().getId());
                        delete(billDocumentOpt.get());
                        return true;
                } else {
                        return false;
                }
        }

}
