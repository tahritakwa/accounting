package fr.sparkit.accounting.services.impl;

import fr.sparkit.accounting.auditing.DocumentAccountExcelCell;
import fr.sparkit.accounting.constants.FilterConstants;
import fr.sparkit.accounting.constants.LanguageConstants;
import fr.sparkit.accounting.constants.NumberConstant;
import fr.sparkit.accounting.constants.XLSXErrors;
import fr.sparkit.accounting.convertor.AccountConvertor;
import fr.sparkit.accounting.convertor.DocumentAccountingConvertor;
import fr.sparkit.accounting.convertor.DocumentAccountingLineConvertor;
import fr.sparkit.accounting.convertor.FiscalYearConvertor;
import fr.sparkit.accounting.dao.BillDocumentDao;
import fr.sparkit.accounting.dao.DocumentAccountDao;
import fr.sparkit.accounting.dao.DocumentAccountLineDao;
import fr.sparkit.accounting.dto.*;
import fr.sparkit.accounting.dto.excel.DocumentAccountXLSXFormatDto;
import fr.sparkit.accounting.entities.*;
import fr.sparkit.accounting.enumuration.DocumentAccountStatus;
import fr.sparkit.accounting.enumuration.FiscalYearClosingState;
import fr.sparkit.accounting.services.*;
import fr.sparkit.accounting.services.utils.AccountingServiceUtil;
import fr.sparkit.accounting.services.utils.excel.ExcelCellStyleHelper;
import fr.sparkit.accounting.services.utils.excel.GenericExcelPOIHelper;
import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.errors.ErrorsResponse;
import fr.sparkit.accounting.util.http.HttpCustomException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static fr.sparkit.accounting.constants.AccountingConstants.*;
import static fr.sparkit.accounting.services.utils.excel.ExcelCellStyleHelper.setInvalidCell;

@Service
@Slf4j
public class DocumentAccountService extends GenericService<DocumentAccount, Long> implements IDocumentAccountService {
        private static final String TOTAL_DEBIT_AMOUNT = "totalDebitAmount";
        private static final String LINE_ACCOUNT_ID = "lineAccountId";
        private static final String DEBIT_AMOUNT = "debitAmount";
        private static final String CREDIT_AMOUNT = "creditAmount";
        private static final String DOCUMENT_ACCOUNT_SHEET_NAME = "Pièces comptables";
        private static final DataFormatter dataFormatter = new DataFormatter();
        private final List<Field> excelHeaderFields;

        private final List<String> acceptedHeaders;
        private final IJournalService journalService;
        private final IFiscalYearService fiscalYearService;
        private final IDocumentAccountLineService documentAccountLineService;
        private final IAccountingConfigurationService accountingConfigurationService;
        private final IAccountService accountService;
        private final BillDocumentDao billDocumentDao;
        private final DocumentAccountDao documentAccountDao;
        private final DocumentAccountLineDao documentAccountLineDao;
        private final IDepreciationAssetService depreciationAssetsService;
        private final IDepreciationAssetConfigurationService depreciationAssetConfigurationService;
        private final IAmortizationtableService amortizationTableService;
        @Value("${accounting.excel.storage-directory}") private Path excelStoragePath;

        @Autowired
        public DocumentAccountService(IDocumentAccountLineService documentAccountLineService,
                IJournalService journalService, IFiscalYearService fiscalYearService,
                IAccountingConfigurationService accountingConfigurationService, IAccountService accountService,
                BillDocumentDao billDocumentDao, DocumentAccountDao documentAccountDao,
                IDepreciationAssetService depreciationAssetsService,
                IDepreciationAssetConfigurationService depreciationAssetConfigurationService,
                IChartAccountsService chartAccountsService, IAmortizationtableService amortizationTableService,
                DocumentAccountLineDao documentAccountLineDao) {
                super();
                this.documentAccountDao = documentAccountDao;
                this.documentAccountLineService = documentAccountLineService;
                this.journalService = journalService;
                this.fiscalYearService = fiscalYearService;
                this.accountingConfigurationService = accountingConfigurationService;
                this.accountService = accountService;
                this.billDocumentDao = billDocumentDao;
                this.depreciationAssetsService = depreciationAssetsService;
                this.depreciationAssetConfigurationService = depreciationAssetConfigurationService;
                this.documentAccountLineDao = documentAccountLineDao;
                this.amortizationTableService = amortizationTableService;
                excelHeaderFields = DocumentAccountXLSXFormatDto.getDocumentAccountExcelHeaderFields();
                acceptedHeaders = excelHeaderFields.stream()
                        .map(field -> field.getAnnotation(DocumentAccountExcelCell.class).headerName())
                        .collect(Collectors.toList());
        }

        private static void checkNull(Object obj) {
                if (obj == null) {
                        throw new HttpCustomException(ApiErrors.Accounting.DOCUMENT_ACCOUNT_MISSING_PARAMETERS);
                }
        }

        @Override
        public boolean isValidDocumentAccount(DocumentAccountingDto documentAccountingDto) {
                List<DocumentAccountLineDto> documentAccountLineDto = documentAccountingDto.getDocumentAccountLines()
                        .stream().filter((DocumentAccountLineDto documentAccountLine) ->
                                (documentAccountLine.getCreditAmount().compareTo(BigDecimal.ZERO) == 0
                                        && documentAccountLine.getDebitAmount().compareTo(BigDecimal.ZERO) != 0) || (
                                        documentAccountLine.getCreditAmount().compareTo(BigDecimal.ZERO) != 0
                                                && documentAccountLine.getDebitAmount().compareTo(BigDecimal.ZERO)
                                                == 0)).collect(Collectors.toList());
                documentAccountingDto.setDocumentAccountLines(documentAccountLineDto);
                return !documentAccountIsEmpty(documentAccountingDto) && totalDebitAmountIsEqualToTotalCreditAmount(
                        documentAccountingDto) && documentAccountHasValidLines(documentAccountingDto);
        }

        @Override
        public boolean totalDebitAmountIsEqualToTotalCreditAmount(DocumentAccountingDto documentAccountingDto) {
                BigDecimal totalDebitAmount = calculateTotalDebitAmountDocument(documentAccountingDto);
                BigDecimal totalCreditAmount = calculateTotalCreditAmountDocument(documentAccountingDto);
                return totalDebitAmount.compareTo(totalCreditAmount) == 0;
        }

        @Override
        public boolean documentAccountIsEmpty(DocumentAccountingDto documentAccountingDto) {
                return documentAccountingDto.getDocumentAccountLines().isEmpty();
        }

        @Override
        public boolean documentAccountHasValidLines(DocumentAccountingDto documentAccountingDto) {
                Set<Long> accountIds = documentAccountingDto.getDocumentAccountLines().stream()
                        .map(DocumentAccountLineDto::getAccountId).collect(Collectors.toSet());
                accountIds.forEach((Long accountId) -> {
                        if (!accountService.existsById(accountId)) {
                                throw new HttpCustomException(
                                        ApiErrors.Accounting.DOCUMENT_ACCOUNT_LINE_ACCOUNT_DOES_NOT_EXIST,
                                        new ErrorsResponse().error(accountId));
                        }
                });
                for (DocumentAccountLineDto documentAccountLine : documentAccountingDto.getDocumentAccountLines()) {
                        if (!accountService.existsById(documentAccountLine.getAccountId())) {
                                throw new HttpCustomException(
                                        ApiErrors.Accounting.DOCUMENT_ACCOUNT_LINE_ACCOUNT_DOES_NOT_EXIST,
                                        new ErrorsResponse().error(documentAccountLine.getAccountId()));
                        }
                        if (documentAccountLine.getCreditAmount().compareTo(BigDecimal.ZERO) == 0
                                && documentAccountLine.getDebitAmount().compareTo(BigDecimal.ZERO) == 0) {
                                return false;
                        }
                }
                return true;
        }

        @Override
        public BigDecimal calculateTotalDebitAmountDocument(DocumentAccountingDto documentAccountingDto) {
                BigDecimal totalDebitAmount = BigDecimal.ZERO;
                for (DocumentAccountLineDto documentAccountLine : documentAccountingDto.getDocumentAccountLines()) {
                        totalDebitAmount = totalDebitAmount.add(documentAccountLine.getDebitAmount()
                                .setScale(DEFAULT_SCALE_FOR_BIG_DECIMAL, RoundingMode.HALF_UP));
                }
                return totalDebitAmount;
        }

        @Override
        public BigDecimal calculateTotalCreditAmountDocument(DocumentAccountingDto documentAccountingDto) {
                BigDecimal totalCreditAmount = BigDecimal.ZERO;

                for (DocumentAccountLineDto documentAccountLine : documentAccountingDto.getDocumentAccountLines()) {
                        totalCreditAmount = totalCreditAmount.add(documentAccountLine.getCreditAmount()
                                .setScale(DEFAULT_SCALE_FOR_BIG_DECIMAL, RoundingMode.HALF_UP));
                }
                return totalCreditAmount;
        }

        @Override
        public boolean isNewDocumentAccount(DocumentAccountingDto documentAccountingDto) {
                return documentAccountingDto.getId() == null;
        }

        @Caching(evict = { @CacheEvict(value = "GeneralLedgerAccounts", allEntries = true),
                @CacheEvict(value = "GeneralLedgerAccountDetails", allEntries = true),
                @CacheEvict(value = "TrialBalanceAccounts", allEntries = true) })
        @Override
        public DocumentAccount saveDocumentAccount(DocumentAccountingDto documentAccountingDto,
                boolean isComingFromMovingToNextFiscalYear) {
                Long fiscalYearId = getFiscalYear(documentAccountingDto);
                DocumentAccount documentAccount = null;
                FiscalYear fiscalYear = fiscalYearService.findOne(fiscalYearId);

                if (!fiscalYearService.isDateInClosedPeriod(documentAccountingDto.getDocumentDate(), fiscalYearId)) {
                        if (isValidDocumentAccount(documentAccountingDto)) {
                                Journal journal = journalService.findOne(documentAccountingDto.getJournalId());
                                AccountingConfigurationDto currentConfiguration = accountingConfigurationService
                                        .findLastConfig();
                                FiscalYearDto currentFiscalYear = accountingConfigurationService.getCurrentFiscalYear();
                                if (documentAccountingDto.getBillId() == null) {
                                        isValidDocumentDate(documentAccountingDto.getDocumentDate(),
                                                journal.getId().equals(currentConfiguration.getJournalANewId()),
                                                currentFiscalYear);
                                        documentAccountingDto.getDocumentAccountLines().forEach(
                                                documentAccountLineDto -> isValidDocumentDate(
                                                        documentAccountLineDto.getDocumentLineDate(),
                                                        journal.getId().equals(currentConfiguration.getJournalANewId()),
                                                        currentFiscalYear));
                                }
                                documentAccount = DocumentAccountingConvertor
                                        .dtoToModel(documentAccountingDto, journal);
                                documentAccount.setFiscalYear(
                                        FiscalYearConvertor.dtoToModel(fiscalYearService.findById(fiscalYearId)));
                                if (isNewDocumentAccount(documentAccountingDto)) {
                                        fillAutoGeneratedValues(isComingFromMovingToNextFiscalYear, documentAccount);
                                } else {
                                        DocumentAccount old = documentAccountDao.findOne(documentAccountingDto.getId());
                                        checkPermissionsToUpdateDocument(documentAccountingDto,
                                                isComingFromMovingToNextFiscalYear, documentAccount, old);
                                        documentAccount.setId(old.getId());
                                        documentAccount.setCreationDocumentDate(old.getCreationDocumentDate());
                                        deleteUnsavedDocumentAccountLines(documentAccountingDto);
                                        log.info(DOCUMENT_ACCOUNT_LINES_DELETED);
                                }
                                documentAccount.setIndexOfStatus(documentAccountingDto.getIndexOfStatus());
                                documentAccount.setDocumentDate(
                                        documentAccount.getDocumentDate().toLocalDate().atStartOfDay());
                                documentAccount = documentAccountDao.saveAndFlush(documentAccount);
                                saveDocumentAccountLines(documentAccountingDto.getDocumentAccountLines(),
                                        documentAccount);
                                log.info(DOCUMENT_ACCOUNT_LINES_SAVED);
                        } else {
                                        log.error(TRYING_TO_SAVE_DA_WITH_INCORRECT_AMOUNT);
                                        throw new HttpCustomException(
                                                ApiErrors.Accounting.DOCUMENT_ACCOUNT_AMOUNT_CODE);
                        }
                        return documentAccount;
                } else {
                        log.error(TRYING_TO_SAVE_DA_IN_CLOSED_PERIOD);
                        throw new HttpCustomException(ApiErrors.Accounting.DOCUMENT_ACCOUNT_DATE_IN_CLOSED_PERIOD,
                                new ErrorsResponse().error(fiscalYear.getName()));
                }
        }

        private void fillAutoGeneratedValues(boolean isComingFromMovingToNextFiscalYear,
                DocumentAccount documentAccount) {
                documentAccount.setCodeDocument(generateRandomCode(documentAccount.getDocumentDate()));
                documentAccount.setCreationDocumentDate(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
                if (isComingFromMovingToNextFiscalYear) {
                        documentAccount.setIndexOfStatus(
                                DocumentAccountStatus.BY_CONCLUDING_CURRENT_FISCAL_YEAR_IS_CREATED.getIndex());
                }
        }

        private void checkPermissionsToUpdateDocument(DocumentAccountingDto documentAccountingDto,
                boolean isComingFromMovingToNextFiscalYear, DocumentAccount documentAccount, DocumentAccount old) {
                if (old == null) {
                        log.error(NOT_FOUND_DOCUMENT_ACCOUNT, documentAccountingDto.getId());
                        throw new HttpCustomException(ApiErrors.Accounting.ENTITY_DOCUMENT_ACCOUNT_NOT_FOUND,
                                new ErrorsResponse().error(documentAccountingDto.getCodeDocument()));
                }
                if (old.getIndexOfStatus() == DocumentAccountStatus.BY_CONCLUDING_CURRENT_FISCAL_YEAR_IS_CREATED
                        .getIndex() && !isComingFromMovingToNextFiscalYear) {
                        log.error(DOCUMENT_ACCOUNT_COMING_FROM_CLOSING_FISCAL_YEAR_CANNOT_BE_MANUALLY_UPDATED);
                        throw new HttpCustomException(
                                ApiErrors.Accounting.DOCUMENT_ACCOUNT_COMING_FROM_CLOSING_FISCAL_YEAR_CANNOT_BE_MANUALLY_UPDATED);
                }
                if (old.getIndexOfStatus() == DocumentAccountStatus.BY_IMPORT_DOCUMENT_IS_CREATED.getIndex()
                        || old.getIndexOfStatus() == DocumentAccountStatus.BY_IMPORT_SETTLEMENT_IS_CREATED.getIndex()) {
                        log.error(DOCUMENT_ACCOUNT_COMING_FROM_A_BILL_CANNOT_BE_MANUALLY_UPDATED);
                        throw new HttpCustomException(
                                ApiErrors.Accounting.DOCUMENT_ACCOUNT_COMING_FROM_A_BILL_CANNOT_BE_MANUALLY_UPDATED);
                }
                if (fiscalYearService.isDateInClosedPeriod(old.getDocumentDate(), old.getFiscalYear().getId())) {
                        log.error("Trying to update a DocumentAccount that is in a closed period");
                        throw new HttpCustomException(
                                ApiErrors.Accounting.DOCUMENT_ACCOUNT_CANT_UPDATE_DOCUMENT_IN_CLOSED_PERIOD);
                }
                Optional<DocumentAccount> documentWithSameCode = documentAccountDao
                        .findByCodeDocumentAndFiscalYearIdAndIsDeletedFalse(documentAccount.getCodeDocument(),
                                old.getFiscalYear().getId());
                if (documentWithSameCode.isPresent() && !documentWithSameCode.get().getId()
                        .equals(documentAccountingDto.getId())) {
                        log.error("Trying to update a DocumentAccount using an existing code");
                        throw new HttpCustomException(ApiErrors.Accounting.DOCUMENT_ACCOUNT_CODE_EXISTS);
                }
        }

        private Long getFiscalYear(DocumentAccountingDto documentAccountingDto) {
                Long fiscalYearId = fiscalYearService.findFiscalYearOfDate(documentAccountingDto.getDocumentDate());
                if (fiscalYearId != null) {
                        return fiscalYearId;
                } else {
                        log.error(DOCUMENT_ACCOUNT_NO_FISCAL_YEAR);
                        throw new HttpCustomException(ApiErrors.Accounting.DOCUMENT_ACCOUNT_NO_FISCAL_YEAR,
                                new ErrorsResponse().error(DateTimeFormatter.ISO_LOCAL_DATE
                                        .format(documentAccountingDto.getDocumentDate())));
                }
        }

        private String generateRandomCode(LocalDateTime documentDate) {
                int month = documentDate.getMonthValue();
                int indexCode = NumberConstant.ONE;
                LocalDate firstOfTheMonth = LocalDate
                        .of(documentDate.getYear(), documentDate.getMonth(), NumberConstant.ONE);
                LocalDate endOfTheMonth = firstOfTheMonth
                        .plusDays((long) firstOfTheMonth.lengthOfMonth() - NumberConstant.ONE);

                DocumentAccount lastDocumentAccountForDate = documentAccountDao
                        .findFirstByDocumentDateBetweenAndIsDeletedFalseOrderByCodeDocumentDesc(
                                firstOfTheMonth.atTime(LocalTime.MIN),
                                endOfTheMonth.atTime(LocalTime.MAX).truncatedTo(ChronoUnit.MILLIS));
                if (lastDocumentAccountForDate != null) {
                        String lastCode = lastDocumentAccountForDate.getCodeDocument();
                        if (lastCode != null && !lastCode.isEmpty()) {
                                indexCode = Integer.parseInt(
                                        lastCode.substring(lastCode.lastIndexOf('/') + NumberConstant.ONE)) + 1;
                        }
                }
                return String.format("%02d", month) + '/' + String
                        .format("%0".concat(DOCUMENT_INDEX_CODE_LENGTH + "d"), indexCode);
        }

        @Override
        public boolean existsById(Long documentAccountId) {
                return documentAccountDao.existsByIdAndIsDeletedFalse(documentAccountId);
        }

        private boolean isRowOfTypeDocument(Row row, int acceptedHeadersSize) {
                for (int cellIndex = 0; cellIndex < acceptedHeadersSize; cellIndex++) {
                        Cell cell = row.getCell(cellIndex);
                        if (cell != null && cell.getCellType() != CellType.BLANK && excelHeaderFields
                                .get(cell.getColumnIndex()).getAnnotation(DocumentAccountExcelCell.class)
                                .isDocumentAccountField() && !dataFormatter.formatCellValue(cell).trim().isEmpty()) {
                                return true;
                        }
                }
                return false;
        }

        @Override
        public boolean isMonetaryValueNegativeOrScaleInvalid(Cell cell, BigDecimal credit) {
                if (credit.compareTo(BigDecimal.ZERO) < 0) {
                        setInvalidCell(cell, XLSXErrors.DocumentAccountXLSXErrors.MONETARY_VALUE_CANT_BE_NEGATIVE);
                        return true;
                }
                if (credit.scale() > DEFAULT_SCALE_FOR_BIG_DECIMAL) {
                        setInvalidCell(cell,
                                String.format(XLSXErrors.DocumentAccountXLSXErrors.MONETARY_VALUE_SCALE_REACHED,
                                        DEFAULT_SCALE_FOR_BIG_DECIMAL));
                        return true;
                }
                return false;
        }

        @Override
        public DocumentAccountingDto getDocumentAccount(Long id) {
                if (!existsById(id)) {
                        throw new HttpCustomException(ApiErrors.Accounting.DOCUMENT_ACCOUNT_NON_EXISTENT,
                                new ErrorsResponse().error(id));
                }
                DocumentAccount documentAccount = documentAccountDao.findOne(id);
                FiscalYearDto currentFiscalYear = accountingConfigurationService.getCurrentFiscalYear();
                if (!accountingConfigurationService.isDateInCurrentFiscalYear(documentAccount.getDocumentDate())) {
                        log.error("Document account with id {} and date {} is not in current fiscal year {}",
                                documentAccount.getId(), documentAccount.getDocumentDate(),
                                currentFiscalYear.getName());
                        throw new HttpCustomException(ApiErrors.Accounting.DOCUMENT_ACCOUNT_NOT_IN_FISCAL_YEAR,
                                new ErrorsResponse().error(currentFiscalYear.getName()));
                }
                List<DocumentAccountLine> documentAccountLines = documentAccountLineService.findByDocumentAccountId(id);
                List<DocumentAccountLineDto> documentAccountLinesDto = new ArrayList<>();

                documentAccountLines.forEach((DocumentAccountLine documentAccountLine) -> documentAccountLinesDto
                        .add(DocumentAccountingLineConvertor.modelToDto(documentAccountLine)));
                BigDecimal totalDebitAmount = documentAccountLineService.totalDebitAmountDocumentAccount(id)
                        .orElse(BigDecimal.ZERO);
                BigDecimal totalcreditAmount = documentAccountLineService.totalCreditAmountDocumentAccount(id)
                        .orElse(BigDecimal.ZERO);
                return DocumentAccountingConvertor
                        .modelToDto(documentAccount, totalDebitAmount, totalcreditAmount, documentAccountLinesDto);
        }

        @Caching(evict = { @CacheEvict(value = "GeneralLedgerAccounts", allEntries = true),
                @CacheEvict(value = "GeneralLedgerAccountDetails", allEntries = true),
                @CacheEvict(value = "TrialBalanceAccounts", allEntries = true) })
        @Override
        public boolean deleteDocumentAccount(Long id) {
                DocumentAccount documentAccountToBeDeleted = checkIfCanDelete(id);
                if (documentAccountToBeDeleted.getIndexOfStatus() == DocumentAccountStatus.BY_IMPORT_DOCUMENT_IS_CREATED
                        .getIndex() || documentAccountToBeDeleted.getIndexOfStatus()
                        == DocumentAccountStatus.BY_IMPORT_SETTLEMENT_IS_CREATED.getIndex()) {
                        log.error(DOCUMENT_ACCOUNT_COMING_FROM_BILL_CANNOT_BE_DELETED, id);
                        throw new HttpCustomException(
                                ApiErrors.Accounting.DOCUMENT_ACCOUNT_COMING_FROM_BILL_CANNOT_BE_DELETED);
                }
                return deleteDocument(id, documentAccountToBeDeleted);
        }

        @Override
        public boolean deleteDocument(Long id, DocumentAccount documentAccountToBeDeleted) {
                FiscalYearDto currentFiscalYear = accountingConfigurationService.getCurrentFiscalYear();
                if (!fiscalYearService.isDateInClosedPeriod(documentAccountToBeDeleted.getDocumentDate(),
                        currentFiscalYear.getId())) {
                        List<DocumentAccountLine> documentAccountLines = documentAccountLineService
                                .findByDocumentAccountId(id);
                        documentAccountLineService.deleteInBatchSoft(documentAccountLines);
                        delete(documentAccountToBeDeleted);
                        log.info(LOG_ENTITY_DELETED, ENTITY_NAME_DOCUMENT_ACCOUNT, id);
                        log.info(LOG_ENTITY_DELETED, ENTITY_NAME_DOCUMENT_ACCOUNT_LINES,
                                documentAccountLines.stream().map(DocumentAccountLine::getId).toArray());
                        return true;
                } else {
                        log.error("Trying to delete a DocumentAccount that is in a closed period");
                        throw new HttpCustomException(
                                ApiErrors.Accounting.DOCUMENT_ACCOUNT_CANT_DELETE_DOCUMENT_IN_CLOSED_PERIOD);
                }
        }

        @Override
        public DocumentAccount checkIfCanDelete(Long id) {
                checkNull(id);
                if (!existsById(id)) {
                        log.error(DOCUMENT_ACCOUNT_NON_EXISTENT, id);
                        throw new HttpCustomException(ApiErrors.Accounting.DOCUMENT_ACCOUNT_NON_EXISTENT,
                                new ErrorsResponse().error(id));
                }
                DocumentAccount documentAccountToBeDeleted = documentAccountDao.findOne(id);
                if (documentAccountToBeDeleted.getIndexOfStatus()
                        == DocumentAccountStatus.BY_CONCLUDING_CURRENT_FISCAL_YEAR_IS_CREATED.getIndex()) {
                        log.error(DOCUMENT_ACCOUNT_COMING_FROM_CLOSING_FISCAL_YEAR_CANNOT_BE_DELETED, id);
                        throw new HttpCustomException(
                                ApiErrors.Accounting.DOCUMENT_ACCOUNT_COMING_FROM_CLOSING_FISCAL_YEAR_CANNOT_BE_DELETED);
                }
                if (hasDocumentAccountLetteredLines(documentAccountToBeDeleted)) {
                        log.error(DOCUMENT_ACCOUNT_CONTAINS_LETTERED_LINES_CANNOT_BE_DELETED);
                        throw new HttpCustomException(
                                ApiErrors.Accounting.DOCUMENT_ACCOUNT_CONTAINS_LETTERED_LINES_CANNOT_BE_DELETED);
                }
                if (hasDocumentAccountReconcilableLines(documentAccountToBeDeleted)) {
                        log.error(DOCUMENT_ACCOUNT_CONTAINS_RECONCILABLE_LINES_CANNOT_BE_DELETED, id);
                        throw new HttpCustomException(
                                ApiErrors.Accounting.DOCUMENT_ACCOUNT_CONTAINS_RECONCILABLE_LINES_CANNOT_BE_DELETED);
                }
                return documentAccountToBeDeleted;
        }

        @Override
        public boolean hasDocumentAccountLetteredLines(DocumentAccount documentAccount) {
                return documentAccountLineService.getCountLetteredLinesByDocumentAccountId(documentAccount.getId()) > 0;
        }

        private boolean hasDocumentAccountReconcilableLines(DocumentAccount documentAccount) {
                return documentAccountLineService.getTotalReconcilableLinesByDocumentAccountId(documentAccount.getId())
                        > 0;
        }

        @Override
        public void saveDocumentAccountLines(List<DocumentAccountLineDto> documentAccountLines,
                DocumentAccount documentAccount) {
                documentAccountLineService.save(documentAccountLines, documentAccount);
        }

        @Override
        public List<DocumentAccount> findByJournal(Long journalId, LocalDateTime startDate, LocalDateTime endDate) {
                return documentAccountDao
                        .findByJournalIdAndDocumentDateBetweenAndIsDeletedFalse(journalId, startDate, endDate);
        }

        @Override
        public void deleteUnsavedDocumentAccountLines(DocumentAccountingDto documentAccountingDto) {

                List<Long> documentAccountLinesIdToSave = new ArrayList<>();

                documentAccountingDto.getDocumentAccountLines()
                        .forEach(documentAccountLine -> documentAccountLinesIdToSave.add(documentAccountLine.getId()));

                List<DocumentAccountLine> documentAccountLines = documentAccountLineService
                        .findByDocumentAccountId(documentAccountingDto.getId());
                documentAccountLines.stream()
                        .filter((DocumentAccountLine documentAccountLine) -> !documentAccountLinesIdToSave
                                .contains(documentAccountLine.getId())).forEach(
                        (DocumentAccountLine documentAccountLineToDelete) -> documentAccountLineService
                                .delete(documentAccountLineToDelete));

        }

        public Pageable getDefaultPageableUsingDocumentDateAndCodeDocumentFields(int page, int size,
                String customizedDocumentDateFieldToSortBy, String customizedCodeDocumentFieldToSortBy) {
                return PageRequest.of(page, size,
                        JpaSort.unsafe(Sort.Direction.DESC, customizedDocumentDateFieldToSortBy,
                                customizedCodeDocumentFieldToSortBy));
        }

        @Override
        public List<DocumentAccount> findAllDocumentsInFiscalYear(Long fiscalYearId) {
                return documentAccountDao
                        .findByFiscalYearIdAndIsDeletedFalseOrderByDocumentDateDescCodeDocumentDesc(fiscalYearId);
        }

        @Override
        public DocumentAccountLine getPurchasesTaxStampDocumentLine(BillDto billDto, AccountDto purchasesAccountDto) {
                return new DocumentAccountLine(billDto.getDocumentDate(), billDto.getTierName(),
                        billDto.getCodeDocument(), billDto.getTaxStamp(), BigDecimal.ZERO,
                        AccountConvertor.dtoToModel(purchasesAccountDto, null));

        }

        @Override
        public DocumentAccountLine getSalesTaxStampDocumentLine(BillDto billDto, AccountDto salesAccountDto) {
                return new DocumentAccountLine(billDto.getDocumentDate(), billDto.getTierName(),
                        billDto.getCodeDocument(), BigDecimal.ZERO, billDto.getTaxStamp(),
                        AccountConvertor.dtoToModel(salesAccountDto, null));
        }

        @Override
        public AccountDto getCofferDocumentLine(RegulationDto regulationDto) {
                Integer cofferAccountCode = accountingConfigurationService.findLastConfig()
                        .getCofferIdAccountingAccountCode();
                StringBuilder code = new StringBuilder().append(cofferAccountCode.toString());
                for (int i = code.length(); i < NumberConstant.EIGHT; i++) {
                        code.append(NumberConstant.ZERO);
                }
                return accountService.findByCode(Integer.parseInt(code.toString()));
        }

        @Override
        public String getCodeDocument(LocalDateTime documentDate) {
                return generateRandomCode(documentDate);
        }

        private boolean isValidDocumentDate(LocalDateTime documentDate, boolean isJournalANew,
                FiscalYearDto currentFiscalYear) {
                LocalDateTime startDate = currentFiscalYear.getStartDate();
                LocalDateTime endDate = currentFiscalYear.getEndDate();
                if (AccountingServiceUtil.dateIsAfterOrEquals(documentDate, startDate) && AccountingServiceUtil
                        .isDateBeforeOrEquals(documentDate, endDate)) {
                        return true;
                } else {
                        log.error(TRYING_WITH_VALID_DATE_DOCUMENT_ACCOUNT_INVALID_DATE);
                        throw new HttpCustomException(ApiErrors.Accounting.DOCUMENT_ACCOUNT_INVALID_DATE);
                }
        }

        private void setBillIdForEachDocumentAccountIfIsImported(List<DocumentAccountingDto> documentAccountList) {
                documentAccountList.stream().filter((DocumentAccountingDto documentAccountingDto) ->
                        documentAccountingDto.getIndexOfStatus() == DocumentAccountStatus.BY_IMPORT_DOCUMENT_IS_CREATED
                                .getIndex() || documentAccountingDto.getIndexOfStatus()
                                == DocumentAccountStatus.BY_IMPORT_SETTLEMENT_IS_CREATED.getIndex())
                        .forEach((DocumentAccountingDto documentAccountingDto) -> {
                                Optional<BillDocument> billDocument = billDocumentDao
                                        .findByDocumentAccountIdAndIsDeletedFalse(documentAccountingDto.getId());
                                billDocument.ifPresent(
                                        document -> documentAccountingDto.setBillId(billDocument.get().getIdBill()));
                        });
        }

        @Override
        public DocumentAccount findJournalANewDocumentForFiscalYear(Long fiscalYearId) {
                return documentAccountDao.findByFiscalYearIdAndIsCreatedByConcludingCurrentFiscalYear(fiscalYearId);
        }

        @Override
        public void deleteJournalANewDocumentForFiscalYear(Long fiscalYearId) {
                DocumentAccount journalANewDocument = findJournalANewDocumentForFiscalYear(fiscalYearId);
                if (journalANewDocument != null) {
                        List<DocumentAccountLine> documentAccountLines = documentAccountLineService
                                .findByDocumentAccountId(journalANewDocument.getId());
                        documentAccountLineService.deleteInBatchSoft(documentAccountLines);
                        delete(journalANewDocument.getId());
                }
        }

        @Caching(evict = { @CacheEvict(value = "GeneralLedgerAccounts", allEntries = true),
                @CacheEvict(value = "GeneralLedgerAccountDetails", allEntries = true),
                @CacheEvict(value = "TrialBalanceAccounts", allEntries = true) })
        @Override
        public FileUploadDto loadDocumentAccountsExcelData(FileUploadDto fileUploadDto) {
                List<DocumentAccountingDto> documentAccounts = new ArrayList<>();
                List<List<DocumentAccountLineDto>> documentAccountLines = new ArrayList<>();
                boolean allSheetsAreEmpty;
                boolean documentsAreValid = true;
                Map<DocumentAccountLineDto, Row> linesMap = new HashMap<>();
                ExcelCellStyleHelper.resetStyles();
                try (Workbook workbook = GenericExcelPOIHelper
                        .createWorkBookFromBase64String(fileUploadDto.getBase64Content())) {
                        allSheetsAreEmpty = true;
                        if (workbook.getNumberOfSheets() == 0) {
                                throw new HttpCustomException(ApiErrors.Accounting.EXCEL_EMPTY_FILE);
                        }
                        GenericExcelPOIHelper.validateWorkbookSheetsHeaders(workbook, acceptedHeaders);
                        for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                                log.info("Parsing sheet #{}", sheetIndex + 1);
                                Sheet sheet = workbook.getSheetAt(sheetIndex);
                                boolean isSheetEmpty = GenericExcelPOIHelper.isSheetEmpty(sheet);
                                allSheetsAreEmpty &= isSheetEmpty;
                                if (isSheetEmpty) {
                                        continue;
                                }
                                documentsAreValid &= isDocumentAccountsValuesAddedToSheet(documentAccounts,
                                        documentAccountLines, documentsAreValid, sheet, linesMap);
                        }
                        documentsAreValid &= isDocumentAmountValid(documentAccountLines, linesMap);
                        if (allSheetsAreEmpty) {
                                log.error("Trying to import empty document");
                                throw new HttpCustomException(ApiErrors.Accounting.EXCEL_EMPTY_FILE);
                        } else if (documentsAreValid) {
                                log.info("Saving documents");
                                saveDocumentAccountsComingFromExcel(documentAccounts, documentAccountLines);
                                return new FileUploadDto();
                        } else {
                                return GenericExcelPOIHelper
                                        .getFileUploadDtoFromWorkbook(workbook, excelStoragePath.toFile(),
                                                String.format(SIMULATION_EXPORT_FILE_NAME,
                                                        DOCUMENT_ACCOUNT_SHEET_NAME));
                        }
                } catch (IOException e) {
                        log.error(ERROR_CREATING_FILE, e);
                        throw new HttpCustomException(ApiErrors.Accounting.EXCEL_FILE_CREATION_FAIL);
                }
        }

        private boolean isDocumentAmountValid(List<List<DocumentAccountLineDto>> documentAccountLines,
                Map<DocumentAccountLineDto, Row> linesMap) {
                boolean isDocumentAmountValid = true;
                for (List<DocumentAccountLineDto> accountLine : documentAccountLines) {
                        BigDecimal sumDebitForPreviousDocument = accountLine.stream()
                                .map(DocumentAccountLineDto::getDebitAmount).filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        BigDecimal sumCreditForPreviousDocument = accountLine.stream()
                                .map(DocumentAccountLineDto::getCreditAmount).filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                        if (sumDebitForPreviousDocument.compareTo(sumCreditForPreviousDocument) != 0) {
                                isDocumentAmountValid = false;
                                accountLine.forEach((DocumentAccountLineDto documentAccountLine) -> {
                                        Row row = linesMap.get(documentAccountLine);
                                        Cell errorHeaderCell = row.getSheet().getRow(0)
                                                .createCell(excelHeaderFields.size());
                                        errorHeaderCell.setCellStyle(
                                                ExcelCellStyleHelper.getHeaderStyle(row.getSheet().getWorkbook()));
                                        errorHeaderCell.setCellValue(ERRORS_HEADER_NAME);

                                        row.createCell(excelHeaderFields.size());
                                        setInvalidCell(row.getCell(excelHeaderFields.size()),
                                                XLSXErrors.DocumentAccountXLSXErrors.SUM_DEBIT_DIFFERENT_THAN_SUM_CREDIT);
                                        row.getCell(excelHeaderFields.size()).setCellComment(null);
                                        row.getCell(excelHeaderFields.size()).setCellValue(
                                                XLSXErrors.DocumentAccountXLSXErrors.SUM_DEBIT_DIFFERENT_THAN_SUM_CREDIT);
                                        row.getSheet().autoSizeColumn(excelHeaderFields.size());
                                });
                        }
                }
                return isDocumentAmountValid;
        }

        private boolean isDocumentAccountsValuesAddedToSheet(List<DocumentAccountingDto> documentAccounts,
                List<List<DocumentAccountLineDto>> documentAccountLines, boolean documentsAreValid, Sheet sheet,
                Map<DocumentAccountLineDto, Row> linesMap) {
                DocumentAccountingDto documentAccount;
                for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                        log.info("Parsing row #{} in sheet {}", rowIndex, sheet.getSheetName());
                        Row row = sheet.getRow(rowIndex);
                        if (GenericExcelPOIHelper.isRowNotEmpty(row)) {
                                if (isRowOfTypeDocument(row, acceptedHeaders.size())) {
                                        documentAccount = new DocumentAccountingDto();
                                        documentAccountLines.add(new ArrayList<>());
                                        documentAccounts.add(documentAccount);
                                        documentsAreValid &= documentAccountValuesAddedToRow(documentAccount, row,
                                                excelHeaderFields);
                                }
                                if (documentAccounts.isEmpty()) {
                                        log.error("The first data row should contain documentAccount information");
                                        throw new HttpCustomException(
                                                ApiErrors.Accounting.EXCEL_FIRST_ROW_SHOULD_CONTAIN_DOCUMENT_INFORMATION,
                                                new ErrorsResponse().error(sheet.getSheetName()));
                                }
                                DocumentAccountLineDto documentAccountLine = new DocumentAccountLineDto();
                                documentsAreValid &= documentAccountLineService
                                        .isDocumentAccountLineValuesAddedToRow(documentAccountLine, row,
                                                excelHeaderFields, acceptedHeaders);
                                documentsAreValid &= validateDocumentAccountDebitAndCreditValues(documentAccountLine,
                                        row.getCell(acceptedHeaders
                                                .indexOf(LanguageConstants.XLSXHeaders.DEBIT_HEADER_NAME)), row.getCell(
                                                acceptedHeaders
                                                        .indexOf(LanguageConstants.XLSXHeaders.CREDIT_HEADER_NAME)));
                                documentAccountLines.get(documentAccounts.size() - 1).add(documentAccountLine);
                                linesMap.put(documentAccountLine, row);
                        }
                }
                return documentsAreValid;
        }

        private void saveDocumentAccountsComingFromExcel(List<DocumentAccountingDto> documentAccounts,
                List<List<DocumentAccountLineDto>> documentAccountLines) {
                if (documentAccounts.isEmpty()) {
                        throw new HttpCustomException(ApiErrors.Accounting.EXCEL_NO_DOCUMENT_ACCOUNTS_TO_BE_SAVED);
                }
                for (int i = 0; i < documentAccounts.size(); i++) {
                        documentAccounts.get(i).setDocumentAccountLines(documentAccountLines.get(i));
                        saveDocumentAccount(documentAccounts.get(i), false);
                }
        }

        private boolean validateDocumentAccountDebitAndCreditValues(DocumentAccountLineDto documentAccountLine,
                Cell debitCell, Cell creditCell) {
                boolean creditCellIsValid = creditCell != null && creditCell.getCellComment() == null;
                boolean debitCellCellIsValid = debitCell != null && debitCell.getCellComment() == null;
                if (creditCellIsValid && debitCellCellIsValid && !documentAccountLineService
                        .isLineMonetaryValuesValid(documentAccountLine.getDebitAmount(),
                                documentAccountLine.getCreditAmount())) {
                        setInvalidCell(debitCell, XLSXErrors.DEBIT_OR_CREDIT_VALUE_INVALID);
                        setInvalidCell(creditCell, XLSXErrors.DEBIT_OR_CREDIT_VALUE_INVALID);
                        return false;
                }
                return true;

        }

        private boolean documentAccountValuesAddedToRow(DocumentAccountingDto documentAccount, Row row,
                List<Field> excelHeaderFields) {
                boolean isValid = true;
                for (int i = 0; i < excelHeaderFields.size(); i++) {
                        Cell cell = row.getCell(i);
                        if (cell == null) {
                                cell = row.createCell(i);
                        }
                        cell.setCellComment(null);
                        if (excelHeaderFields.get(i).getAnnotation(DocumentAccountExcelCell.class)
                                .isDocumentAccountField()) {
                                switch (excelHeaderFields.get(i).getAnnotation(DocumentAccountExcelCell.class)
                                        .headerName()) {
                                case LanguageConstants.XLSXHeaders.DOCUMENT_DATE_HEADER_NAME:
                                        isValid &= setupDocumentDateFromCell(cell, documentAccount);
                                        break;
                                case LanguageConstants.XLSXHeaders.DOCUMENT_LABEL_HEADER_NAME:
                                        isValid &= setupDocumentLabelFromCell(cell, documentAccount);
                                        break;
                                case LanguageConstants.XLSXHeaders.JOURNAL_HEADER_NAME:
                                        isValid &= setupDocumentJournalFromCell(cell, documentAccount);
                                        break;
                                default:
                                        isValid = false;
                                }
                        }
                }
                return isValid;
        }

        private boolean setupDocumentJournalFromCell(Cell cell, DocumentAccountingDto documentAccount) {
                if (isJournalInCellValid(cell)) {
                        String journalCode = dataFormatter.formatCellValue(cell).trim();
                        documentAccount.setJournalId(journalService.findByCode(journalCode).getId());
                        return true;
                }
                return false;
        }

        public boolean setupDocumentLabelFromCell(Cell cell, DocumentAccountingDto documentAccount) {
                if (GenericExcelPOIHelper.isLabelInCellValid(cell)) {
                        documentAccount.setLabel(dataFormatter.formatCellValue(cell).trim());
                        return true;
                }
                return false;
        }

        private boolean setupDocumentDateFromCell(Cell cell, DocumentAccountingDto documentAccount) {
                if (isDocumentDateInCellValid(cell)) {
                        LocalDateTime documentDate = LocalDateTime
                                .ofInstant(cell.getDateCellValue().toInstant(), ZoneId.systemDefault());
                        documentAccount.setDocumentDate(documentDate);
                        return true;
                }
                return false;
        }

        @Override
        public boolean isDocumentDateInCellValid(Cell cell) {
                if (!dataFormatter.formatCellValue(cell).trim().isEmpty()) {
                        if (cell.getCellType().equals(CellType.NUMERIC) && DateUtil.isCellDateFormatted(cell)) {
                                Date cellDateValue = cell.getDateCellValue();
                                if (cellDateValue != null) {
                                        return isDocumentDateInFiscalYearAndNotInClosedPeriod(cell, cellDateValue);
                                } else {
                                        setInvalidCell(cell, XLSXErrors.REQUIRED_FIELD);
                                }
                        } else {
                                setInvalidCell(cell,
                                        XLSXErrors.DocumentAccountXLSXErrors.DOCUMENT_DATE_CELL_TYPE_SHOULD_BE_DATE);
                        }
                } else {
                        setInvalidCell(cell, XLSXErrors.REQUIRED_FIELD);
                }
                return false;
        }

        private boolean isJournalInCellValid(Cell cell) {
                String journalCode = dataFormatter.formatCellValue(cell).trim();
                if (journalCode.isEmpty()) {
                        setInvalidCell(cell, XLSXErrors.REQUIRED_FIELD);
                } else {
                        Journal journal = journalService.findByCode(journalCode);
                        if (journal == null) {
                                setInvalidCell(cell,
                                        String.format(XLSXErrors.JournalXLSXErrors.NO_JOURNAL_WITH_CODE, journalCode));
                        } else {
                                return true;
                        }
                }
                return false;
        }

        private boolean isDocumentDateInFiscalYearAndNotInClosedPeriod(Cell cell, Date cellDateValue) {
                LocalDateTime documentDate = LocalDateTime.ofInstant(cellDateValue.toInstant(), ZoneId.systemDefault());
                FiscalYearDto currentFiscalYear = accountingConfigurationService.getCurrentFiscalYear();
                if (!AccountingServiceUtil.dateIsAfterOrEquals(documentDate, currentFiscalYear.getStartDate())
                        || !AccountingServiceUtil.isDateBeforeOrEquals(documentDate, currentFiscalYear.getEndDate())) {
                        setInvalidCell(cell,
                                XLSXErrors.DocumentAccountXLSXErrors.DOCUMENT_ACCOUNT_DATE_NOT_IN_FISCAL_YEAR);
                } else {
                        if (fiscalYearService.isDateInClosedPeriod(documentDate, currentFiscalYear.getId())) {
                                setInvalidCell(cell,
                                        XLSXErrors.DocumentAccountXLSXErrors.DOCUMENT_ACCOUNT_DATE_IN_CLOSED_PERIOD);
                        } else {
                                return true;
                        }
                }
                return false;
        }

        @Override
        public byte[] exportDocumentAccountExcelModel() {
                File file = GenericExcelPOIHelper.generateXLSXFileFromData(new ArrayList<>(),
                        String.format(IMPORT_MODEL_FILE_NAME, DOCUMENT_ACCOUNT_SHEET_NAME), excelStoragePath.toFile(),
                        acceptedHeaders, excelHeaderFields, DOCUMENT_ACCOUNT_SHEET_NAME);
                return GenericExcelPOIHelper.convertFileToByteArray(file);
        }

        @Override
        public byte[] exportDocumentAccountsAsExcelFile() {
                List<DocumentAccount> documentAccounts = findAllDocumentsInFiscalYear(
                        accountingConfigurationService.getCurrentFiscalYearId());
                List<Object> documentAccountXLSXFormatDtoList = new ArrayList<>();
                for (DocumentAccount documentAccount : documentAccounts) {
                        createDocumentAccountXLSXFormatDto(documentAccountXLSXFormatDtoList, documentAccount);
                }
                File file = GenericExcelPOIHelper.generateXLSXFileFromData(documentAccountXLSXFormatDtoList,
                        String.format(EXPORT_FILE_NAME, DOCUMENT_ACCOUNT_SHEET_NAME), excelStoragePath.toFile(),
                        acceptedHeaders, excelHeaderFields, DOCUMENT_ACCOUNT_SHEET_NAME);
                return GenericExcelPOIHelper.convertFileToByteArray(file);
        }

        private void createDocumentAccountXLSXFormatDto(List<Object> documentAccountXLSXFormatDtoList,
                DocumentAccount documentAccount) {
                List<DocumentAccountLine> documentAccountLines = documentAccountLineService
                        .findByDocumentAccountId(documentAccount.getId());

                boolean isDocumentInfoSet = false;
                DocumentAccountXLSXFormatDto documentAccountXLSXFormatDto = new DocumentAccountXLSXFormatDto();
                for (DocumentAccountLine documentAccountLine : documentAccountLines) {
                        if (!isDocumentInfoSet) {
                                documentAccountXLSXFormatDto.setDocumentLabel(documentAccount.getLabel());
                                documentAccountXLSXFormatDto.setJournal(documentAccount.getJournal().getCode());
                                documentAccountXLSXFormatDto.setDocumentDate(documentAccount.getDocumentDate());
                                isDocumentInfoSet = true;
                        } else {
                                documentAccountXLSXFormatDto = new DocumentAccountXLSXFormatDto();
                        }
                        documentAccountXLSXFormatDto.setAccountCode(documentAccountLine.getAccount().getCode());
                        documentAccountXLSXFormatDto
                                .setDocumentAccountLineReference(documentAccountLine.getReference());
                        documentAccountXLSXFormatDto.setDocumentAccountLineLabel(documentAccountLine.getLabel());
                        documentAccountXLSXFormatDto
                                .setDocumentAccountLineDate(documentAccountLine.getDocumentLineDate());
                        documentAccountXLSXFormatDto
                                .setDocumentAccountLineDebitValue(documentAccountLine.getDebitAmount());
                        documentAccountXLSXFormatDto
                                .setDocumentAccountLineCreditValue(documentAccountLine.getCreditAmount());
                        documentAccountXLSXFormatDtoList.add(documentAccountXLSXFormatDto);
                }
        }

        @Override
        public DocumentPageDto filterDocumentAccount(List<Filter> filters, Pageable pageable) {
                long size = documentAccountDao.findNumberOfDocumentsInCurrentFiscalYear(
                        accountingConfigurationService.getCurrentFiscalYearId());
                if (size == 0) {
                        return new DocumentPageDto(new ArrayList<>(), 0L);
                }
                Pageable page;
                Optional<Sort.Order> sortOrder = pageable.getSort().get().findFirst();
                if (!sortOrder.isPresent()) {
                        page = getDefaultPageableUsingDocumentDateAndCodeDocumentFields(0, (int) size,
                                FIELD_NAME_DOCUMENT_DATE, FIELD_NAME_CODE_DOCUMENT);
                } else {
                        if (TOTAL_DEBIT_AMOUNT.equals(sortOrder.get().getProperty())) {
                                page = PageRequest.of(0, (int) size);
                        } else {
                                page = PageRequest.of(0, (int) size, pageable.getSort());
                        }
                }
                List<DocumentAccount> documentsFiltredByCredit = new ArrayList<>();
                List<DocumentAccount> documentsFiltredByDebit = new ArrayList<>();
                List<DocumentAccount> documentsFiltredByAccountId = new ArrayList<>();
                List<DocumentAccount> combinedDocumentList = FilterService

                        .getPageOfFilterableEntity(DocumentAccount.class, documentAccountDao, filters, page)
                        .getContent();
                Optional<Filter> debitAmountFilter = filters.stream()
                        .filter((Filter filter) -> filter.getField().equals(DEBIT_AMOUNT)).findFirst();
                Optional<Filter> creditAmountFilter = filters.stream()
                        .filter((Filter filter) -> filter.getField().equals(CREDIT_AMOUNT)).findFirst();
                Optional<Filter> lineAccountFilter = filters.stream()
                        .filter((Filter filter) -> filter.getField().equals(LINE_ACCOUNT_ID)).findFirst();
                if (debitAmountFilter.isPresent() || creditAmountFilter.isPresent() || lineAccountFilter.isPresent()) {
                        List<DocumentAccountLine> documentAccountLines = new ArrayList<>();

                        List<Long> documentIds = combinedDocumentList.stream().map(DocumentAccount::getId)
                                .collect(Collectors.toList());
                        List<List<Long>> documentIdsPartitions = ListUtils
                                .partition(documentIds, SQL_SERVER_IN_CLAUSE_PARTITION_SIZE);
                        documentIdsPartitions.forEach((List<Long> documentIdsPartition) -> {
                                documentAccountLines.addAll(documentAccountLineDao
                                        .findByDocumentAccountIdInAndIsDeletedFalseOrderByIdDesc(documentIdsPartition));
                        });
                        Set<DocumentAccountLine> documentAccountLinestFiltered = new LinkedHashSet<>();
                        Set<DocumentAccount> documentSet = new LinkedHashSet<>();
                        if (debitAmountFilter.isPresent()) {
                                List<DocumentAccountLine> documentAccountLinesAmountFilter = new ArrayList<>(
                                        documentAccountLines);
                                Predicate<DocumentAccountLine> filterByLineAmountPredicate = (DocumentAccountLine line) -> line
                                        .getDebitAmount().equals(new BigDecimal(debitAmountFilter.get().getValue()));
                                documentsFiltredByDebit = filterDocumentAccountByLinePredicate(combinedDocumentList,
                                        documentAccountLinesAmountFilter, filterByLineAmountPredicate);
                                documentAccountLinestFiltered.addAll(documentAccountLinesAmountFilter);
                                documentSet.addAll(documentsFiltredByDebit);
                        }
                        if (creditAmountFilter.isPresent()) {
                                List<DocumentAccountLine> documentAccountLinesAmountFilter = new ArrayList<>(
                                        documentAccountLines);
                                Predicate<DocumentAccountLine> filterByLineAmountPredicate = (DocumentAccountLine line) -> line
                                        .getCreditAmount().equals(new BigDecimal(creditAmountFilter.get().getValue()));
                                documentsFiltredByCredit = filterDocumentAccountByLinePredicate(combinedDocumentList,
                                        documentAccountLinesAmountFilter, filterByLineAmountPredicate);
                                documentAccountLinestFiltered.addAll(documentAccountLinesAmountFilter);
                                documentSet.addAll(documentsFiltredByCredit);
                        }
                        if (lineAccountFilter.isPresent()) {
                                if (creditAmountFilter.isPresent() && debitAmountFilter.isPresent()) {
                                        documentSet.clear();
                                } else {
                                        documentAccountLinestFiltered.addAll(documentAccountLines);
                                }
                                Predicate<DocumentAccountLine> filterByLineAccountPredicate = (DocumentAccountLine line) -> line
                                        .getAccount().getId()
                                        .equals(Long.parseLong(lineAccountFilter.get().getValue()));
                                documentsFiltredByAccountId = filterDocumentAccountByLinePredicate(combinedDocumentList,
                                        new ArrayList<>(documentAccountLinestFiltered), filterByLineAccountPredicate);
                                documentSet.addAll(documentsFiltredByAccountId);
                        }
                        combinedDocumentList = new ArrayList<>(documentSet);
                }
                List<DocumentAccountingDto> documentAccountingDtos = getDocumentAccountingDtoFromLines(
                        combinedDocumentList);
                if (sortOrder.isPresent() && TOTAL_DEBIT_AMOUNT.equals(sortOrder.get().getProperty())) {
                        documentAccountingDtos
                                .sort((DocumentAccountingDto firstDocument, DocumentAccountingDto secondDocument) -> {
                                        BigDecimal firstDocumentAmount = firstDocument.getTotalDebitAmount()
                                                .max(firstDocument.getTotalCreditAmount());
                                        BigDecimal secondDocumentAmount = secondDocument.getTotalDebitAmount()
                                                .max(secondDocument.getTotalCreditAmount());
                                        return firstDocumentAmount.compareTo(secondDocumentAmount);
                                });
                        if (sortOrder.get().isDescending()) {
                                Collections.reverse(documentAccountingDtos);
                        }
                }
                Optional<Filter> totalDebitAmountFilter = filters.stream()
                        .filter((Filter filter) -> filter.getField().equals(TOTAL_DEBIT_AMOUNT)).findFirst();
                if (totalDebitAmountFilter.isPresent()) {
                        Predicate<DocumentAccountingDto> filterByAmountPredicate = getTotalDebitAmountPredicate(
                                totalDebitAmountFilter.get());
                        if (filterByAmountPredicate != null) {
                                documentAccountingDtos = documentAccountingDtos.stream().filter(filterByAmountPredicate)
                                        .collect(Collectors.toList());
                        }
                }

                long numberOfElements = documentAccountingDtos.size();
                int beginIndex = Math.toIntExact(pageable.getOffset());
                int endIndex = beginIndex + Math.toIntExact(pageable.getPageSize());

                if (beginIndex >= documentAccountingDtos.size()) {
                        beginIndex = 0;
                        endIndex = 0;
                } else if (beginIndex + pageable.getPageSize() > documentAccountingDtos.size()) {
                        endIndex = documentAccountingDtos.size();
                }
                List<DocumentAccountingDto> pageableContent = documentAccountingDtos.subList(beginIndex, endIndex);

                this.setBillIdForEachDocumentAccountIfIsImported(pageableContent);

                return new DocumentPageDto(pageableContent, numberOfElements);
        }

        private List<DocumentAccount> filterDocumentAccountByLinePredicate(List<DocumentAccount> documents,
                List<DocumentAccountLine> documentAccountLines, Predicate<DocumentAccountLine> predicate) {
                List<DocumentAccountLine> filteredLines = documentAccountLines.stream().filter(predicate)
                        .collect(Collectors.toList());
                Set<Long> filteredDocumentAccountLinesIds = filteredLines.stream()
                        .map(DocumentAccountLine::getDocumentAccount).map(DocumentAccount::getId)
                        .collect(Collectors.toSet());
                List<DocumentAccount> filteredDocuments = documents.stream()
                        .filter(document -> filteredDocumentAccountLinesIds.contains(document.getId()))
                        .collect(Collectors.toList());
                documentAccountLines.clear();
                documentAccountLines.addAll(filteredLines);
                return filteredDocuments;
        }

        private List<DocumentAccountingDto> getDocumentAccountingDtoFromLines(List<DocumentAccount> documents) {
                List<DocumentAccountingDto> documentAccountsWithTotals = new ArrayList<>();
                List<DocumentAccountingDto> documentAccountingDtosSorted = new ArrayList<>();
                List<Long> documentIds = documents.stream().map(DocumentAccount::getId).collect(Collectors.toList());

                List<List<Long>> entitiesPartitions = ListUtils
                        .partition(documentIds, SQL_SERVER_IN_CLAUSE_PARTITION_SIZE);
                entitiesPartitions.forEach((List<Long> documentIdsPartition) -> documentAccountsWithTotals
                        .addAll(documentAccountLineDao.getDocumentAccountingDtoByDocumentIds(documentIdsPartition)));
                for (DocumentAccount document : documents) {
                        Optional<DocumentAccountingDto> documentAccount = documentAccountsWithTotals.stream()
                                .filter((DocumentAccountingDto dto) -> dto.getId().equals(document.getId()))
                                .findFirst();
                        if (documentAccount.isPresent()) {
                                documentAccount.get().setCodeDocument(document.getCodeDocument());
                                documentAccount.get().setDocumentDate(document.getDocumentDate());
                                documentAccount.get().setLabel(document.getLabel());
                                documentAccount.get().setJournalId(document.getJournal().getId());
                                documentAccount.get().setJournalLabel(document.getJournal().getLabel());
                                documentAccount.get().setFiscalYearId(document.getFiscalYear().getId());
                                documentAccount.get().setIndexOfStatus(document.getIndexOfStatus());
                                documentAccountingDtosSorted.add(documentAccount.get());
                        }
                }
                return documentAccountingDtosSorted;
        }

        public Predicate<DocumentAccountingDto> getTotalDebitAmountPredicate(Filter totalDebitAmountFilter) {
                Predicate<DocumentAccountingDto> filterByAmountPredicate;
                switch (totalDebitAmountFilter.getOperator()) {
                case FilterConstants.EQUAL_FILTER_OPERATOR:
                        filterByAmountPredicate = (DocumentAccountingDto document) -> document.getTotalDebitAmount()
                                .compareTo(new BigDecimal(totalDebitAmountFilter.getValue())) == NumberConstant.ZERO;
                        break;
                case FilterConstants.NOT_EQUAL_FILTER_OPERATOR:
                        filterByAmountPredicate = (DocumentAccountingDto document) -> document.getTotalDebitAmount()
                                .compareTo(new BigDecimal(totalDebitAmountFilter.getValue())) != NumberConstant.ZERO;
                        break;
                case FilterConstants.IS_NULL_FILTER_OPERATOR:
                case FilterConstants.IS_EMPTY_FILTER_OPERATOR:
                        filterByAmountPredicate = (DocumentAccountingDto document) -> document.getTotalDebitAmount()
                                == null;
                        break;
                case FilterConstants.GREATER_THAN_OR_EQUAL_OPERATOR:
                        filterByAmountPredicate = (DocumentAccountingDto document) -> document.getTotalDebitAmount()
                                .compareTo(new BigDecimal(totalDebitAmountFilter.getValue())) >= NumberConstant.ZERO;
                        break;
                case FilterConstants.GREATER_THAN_OPERATOR:
                        filterByAmountPredicate = (DocumentAccountingDto document) -> document.getTotalDebitAmount()
                                .compareTo(new BigDecimal(totalDebitAmountFilter.getValue())) > NumberConstant.ZERO;
                        break;
                case FilterConstants.LESS_THAN_OR_EQUAL_OPERATOR:
                        filterByAmountPredicate = (DocumentAccountingDto document) -> document.getTotalDebitAmount()
                                .compareTo(new BigDecimal(totalDebitAmountFilter.getValue())) <= NumberConstant.ZERO;
                        break;
                case FilterConstants.LESS_THAN_OPERATOR:
                        filterByAmountPredicate = (DocumentAccountingDto document) -> document.getTotalDebitAmount()
                                .compareTo(new BigDecimal(totalDebitAmountFilter.getValue())) < NumberConstant.ZERO;
                        break;
                default:
                        filterByAmountPredicate = null;
                }
                return filterByAmountPredicate;
        }

        @Override
        public DocumentAccountingDto generateDocumentAccountFromAmortization(Long currentFiscalYearId,
                Long dotationAmortizationAccount, Long journalId, Boolean isDetailedGeneration, String contentType,
                String user, String authorization) {
                if (Boolean.TRUE.equals(isDetailedGeneration)) {
                        return generateDetailedDocumentAccountFromAmortization(currentFiscalYearId,
                                dotationAmortizationAccount, journalId, contentType, user, authorization);
                } else {
                        return generateWithoutDetailDocumentAccountFromAmortization(currentFiscalYearId,
                                dotationAmortizationAccount, journalId, contentType, user, authorization);
                }
        }

        private DocumentAccountingDto generateDetailedDocumentAccountFromAmortization(Long currentFiscalYearId,
                Long dotationAmortizationAccount, Long journalId, String contentType, String user,
                String authorization) {

                Optional<FiscalYear> previousFiscalYear = fiscalYearService.findPreviousFiscalYear(currentFiscalYearId);

                if (previousFiscalYear.isPresent()) {
                        List<AmortizationTable> amortizationTablesOfPreviousFiscalYear = amortizationTableService
                                .findByFiscalYear(previousFiscalYear.get().getId());
                        if (amortizationTablesOfPreviousFiscalYear.isEmpty() && !(previousFiscalYear.get().getClosingState() == FiscalYearClosingState.CONCLUDED.getValue())) {
                                throw new HttpCustomException(ApiErrors.Accounting.AMORTIZATION_OF_ASSETS_NOT_FOUND);
                        }
                }

                List<DepreciationAssetsDto> allDepreciationAssets = depreciationAssetsService
                        .getAllDepreciations(user, contentType, authorization);

                FiscalYearDto currentFiscalYear = fiscalYearService.findById(currentFiscalYearId);

                List<DocumentAccountLineDto> documentAccountLines = new ArrayList<>();

                BigDecimal totalDebitAmount = BigDecimal.ZERO;

                for (DepreciationAssets depreciationAsset : depreciationAssetsService.findAll()) {
                        List<DepreciationAssets> depreciationAssets = new ArrayList<>(Arrays.asList(depreciationAsset));
                        DepreciationAssetsDto depreciationAssetsDto = convertFromDepreciationAssetsJavaToDepreciationAssetsDotNet(
                                depreciationAssets, allDepreciationAssets).stream().findFirst().orElse(null);
                        if (depreciationAssetsDto != null) {
                                AmortizationTableDto amortizationTableDto = amortizationTableService
                                        .getDepreciationOfAsset(depreciationAssetsDto, currentFiscalYear,
                                                depreciationAssetsDto.getIdCategory());
                                BigDecimal annuityExercise = amortizationTableDto.getAnnuityExercise();

                                documentAccountLines.add(new DocumentAccountLineDto(currentFiscalYear.getEndDate(),
                                        String.format(DETAILED_DOCUMENT_ACCOUNT_LABEL_OF_AMORTIZATION,
                                                currentFiscalYear.getName()), depreciationAssetsDto.getAssetsLabel(),
                                        BigDecimal.ZERO, annuityExercise,
                                        depreciationAsset.getAmortizationAccount().getId()));
                                totalDebitAmount = totalDebitAmount.add(annuityExercise);
                        }

                }

                throwExceptionIfNoAssetIsActiveInCurrentFiscalYear(documentAccountLines, totalDebitAmount,
                        currentFiscalYear);

                documentAccountLines.add(new DocumentAccountLineDto(currentFiscalYear.getEndDate(),
                        String.format(DETAILED_DOCUMENT_ACCOUNT_LABEL_OF_AMORTIZATION, currentFiscalYear.getName()),
                        DOCUMENT_ACCOUNT_LINE_REFERENCE_OF_AMORTIZATION, totalDebitAmount, BigDecimal.ZERO,
                        dotationAmortizationAccount));

                deleteAllExistingDocumentAccountGeneratedFromAmortization(currentFiscalYearId);

                DocumentAccountingDto generatedDocumentAccountFromAmortization = saveNewDocumentAccountGeneratedFromAmortization(
                        currentFiscalYearId, journalId, currentFiscalYear, documentAccountLines,
                        DETAILED_DOCUMENT_ACCOUNT_LABEL_OF_AMORTIZATION);

                return generatedDocumentAccountFromAmortization;
        }

        private void deleteAllExistingDocumentAccountGeneratedFromAmortization(Long currentFiscalYearId) {
                List<Long> listOfGeneratedDocumentAccountIdFromAmortization = documentAccountDao
                        .getListOfDocumentAccountGeneratedFromAmortization(currentFiscalYearId);

                if (!listOfGeneratedDocumentAccountIdFromAmortization.isEmpty()) {
                        listOfGeneratedDocumentAccountIdFromAmortization
                                .forEach((Long documentAccountId) -> deleteDocumentAccount(documentAccountId));
                }
        }

        private List<DepreciationAssetsDto> convertFromDepreciationAssetsJavaToDepreciationAssetsDotNet(
                List<DepreciationAssets> depreciationAssets, List<DepreciationAssetsDto> allDepreciationAssets) {
                List<DepreciationAssetsDto> depreciationAssetsDtos = new ArrayList<>();
                for (DepreciationAssets depreciationAsset : depreciationAssets) {
                        Optional<DepreciationAssetsDto> optionnalDepreciationAssetDto = allDepreciationAssets.stream()
                                .filter((DepreciationAssetsDto depreciationAssetDto) -> depreciationAssetDto
                                        .getIdAssets().equals(depreciationAsset.getIdAssets())).findFirst();
                        if (optionnalDepreciationAssetDto.isPresent()) {

                                DepreciationAssetsDto depreciationAssetDto = optionnalDepreciationAssetDto.get();

                                Optional<DepreciationAssetsConfiguration> depreciationConfiguration = Optional
                                        .ofNullable(depreciationAssetConfigurationService
                                                .findByIdCategory(depreciationAssetDto.getIdCategory()));

                                depreciationAssetDto.setIdImmobilizationAccount(
                                        depreciationAsset.getImmobilizationAccount().getId());
                                depreciationAssetDto
                                        .setIdAmortizationAccount(depreciationAsset.getAmortizationAccount().getId());
                                depreciationAssetDto.setCession(depreciationAsset.getCession());
                                if(depreciationAssetDto.getCession()){
                                        depreciationAssetDto.setDateCession(depreciationAsset.getDateCession());
                                }
                                if (depreciationConfiguration.isPresent()) {
                                        depreciationAssetDto.setNbreOfYears(
                                                depreciationConfiguration.get().getDepreciationPeriod());
                                }
                                depreciationAssetsDtos.add(optionnalDepreciationAssetDto.get());
                        }
                }
                return depreciationAssetsDtos;
        }

        boolean isThereOnlyOneAssetByImmobilizationAccount(Long idImmobilizationAccount,
                List<DepreciationAssetsDto> depreciationAssets) {
                return NumberConstant.ONE == (int) depreciationAssets.stream()
                        .filter((DepreciationAssetsDto depricationAsset) -> idImmobilizationAccount
                                .equals(depricationAsset.getIdImmobilizationAccount())).count();
        }

        boolean isThereManyAssetsByImmobilizationAccountHavingDifferentCategory(Long idImmobilizationAccount,
                List<DepreciationAssetsDto> depricationAssets) {
                return NumberConstant.ONE < (int) depricationAssets.stream()
                        .filter((DepreciationAssetsDto depricationAsset) -> idImmobilizationAccount
                                .equals(depricationAsset.getIdImmobilizationAccount()))
                        .map(DepreciationAssetsDto::getIdCategory).distinct().count();
        }

        boolean isThereManyAssetsByImmobilizationAccountHavingSameCategory(Long idImmobilizationAccount,
                List<DepreciationAssetsDto> depricationAssets) {
                return NumberConstant.ONE == (int) depricationAssets.stream()
                        .filter((DepreciationAssetsDto depricationAsset) -> idImmobilizationAccount
                                .equals(depricationAsset.getIdImmobilizationAccount()))
                        .map(DepreciationAssetsDto::getIdCategory).distinct().count();
        }

        private DocumentAccountingDto generateWithoutDetailDocumentAccountFromAmortization(Long currentFiscalYearId,
                Long dotationAmortizationAccount, Long journalId, String contentType, String user,
                String authorization) {

                Optional<FiscalYear> previousFiscalYear = fiscalYearService.findPreviousFiscalYear(currentFiscalYearId);

                if (previousFiscalYear.isPresent()) {
                        List<AmortizationTable> amortizationTablesOfPreviousFiscalYear = amortizationTableService
                                .findByFiscalYear(previousFiscalYear.get().getId());
                        if (amortizationTablesOfPreviousFiscalYear.isEmpty() && !(previousFiscalYear.get().getClosingState() == FiscalYearClosingState.CONCLUDED.getValue())) {
                                throw new HttpCustomException(ApiErrors.Accounting.AMORTIZATION_OF_ASSETS_NOT_FOUND);
                        }
                }

                FiscalYearDto currentFiscalYear = fiscalYearService.findById(currentFiscalYearId);

                List<DocumentAccountLineDto> documentAccountLines = new ArrayList<>();

                BigDecimal totalDebitAmount = BigDecimal.ZERO;

                List<DepreciationAssetsDto> allDepreciationAssetsReturnedFromDotNet = depreciationAssetsService
                        .getAllDepreciations(user, contentType, authorization);

                Map<Account, List<DepreciationAssets>> allDepreciationAssetsMapReturnedFromJava = depreciationAssetsService
                        .findAll().stream()
                        .collect(Collectors.groupingBy(DepreciationAssets::getImmobilizationAccount));

                for (Map.Entry<Account, List<DepreciationAssets>> entry : allDepreciationAssetsMapReturnedFromJava
                        .entrySet()) {

                        Long idImmobilizationAccount = entry.getKey().getId();

                        List<DepreciationAssets> depreciationAssetsReturnedFromJavaByIdImmobilizationAccount = entry
                                .getValue();

                        List<DepreciationAssetsDto> depreciationAssetsReturnedFromDotNetByIdImmobilizationAccount = convertFromDepreciationAssetsJavaToDepreciationAssetsDotNet(
                                depreciationAssetsReturnedFromJavaByIdImmobilizationAccount,
                                allDepreciationAssetsReturnedFromDotNet);

                        if (!depreciationAssetsReturnedFromDotNetByIdImmobilizationAccount.isEmpty()) {
                                Long idAmortizationAccountOfCategory = depreciationAssetConfigurationService
                                        .findByIdCategory(depreciationAssetsReturnedFromDotNetByIdImmobilizationAccount
                                                .get(NumberConstant.ZERO).getIdCategory()).getAmortizationAccount()
                                        .getId();

                                DocumentAccountLineDto documentAccountLine = null;

                                if (isThereOnlyOneAssetByImmobilizationAccount(idImmobilizationAccount,
                                        depreciationAssetsReturnedFromDotNetByIdImmobilizationAccount)) {
                                        documentAccountLine = generateDocumentAccountLine(
                                                depreciationAssetsReturnedFromDotNetByIdImmobilizationAccount,
                                                currentFiscalYear, idAmortizationAccountOfCategory,
                                                idImmobilizationAccount);
                                } else if (isThereManyAssetsByImmobilizationAccountHavingSameCategory(
                                        idImmobilizationAccount,
                                        depreciationAssetsReturnedFromDotNetByIdImmobilizationAccount)) {
                                        documentAccountLine = generateDocumentAccountLine(
                                                depreciationAssetsReturnedFromDotNetByIdImmobilizationAccount,
                                                currentFiscalYear, idAmortizationAccountOfCategory,
                                                idImmobilizationAccount);
                                } else if (isThereManyAssetsByImmobilizationAccountHavingDifferentCategory(
                                        idImmobilizationAccount,
                                        depreciationAssetsReturnedFromDotNetByIdImmobilizationAccount)) {
                                        documentAccountLine = generateDocumentAccountLine(
                                                depreciationAssetsReturnedFromDotNetByIdImmobilizationAccount,
                                                currentFiscalYear,
                                                generateIdAmortizationAccountFromIdImmobilizationAccount(
                                                        idImmobilizationAccount), idImmobilizationAccount);
                                }
                                if (documentAccountLine != null) {
                                        totalDebitAmount = totalDebitAmount.add(documentAccountLine.getCreditAmount());
                                        documentAccountLines.add(documentAccountLine);
                                }
                        }
                }

                throwExceptionIfNoAssetIsActiveInCurrentFiscalYear(documentAccountLines, totalDebitAmount,
                        currentFiscalYear);

                documentAccountLines.add(new DocumentAccountLineDto(currentFiscalYear.getEndDate(),
                        String.format(NOT_DETAILED_DOCUMENT_ACCOUNT_LABEL_OF_AMORTIZATION, currentFiscalYear.getName()),
                        DOCUMENT_ACCOUNT_LINE_REFERENCE_OF_AMORTIZATION, totalDebitAmount, BigDecimal.ZERO,
                        dotationAmortizationAccount));

                deleteAllExistingDocumentAccountGeneratedFromAmortization(currentFiscalYearId);

                DocumentAccountingDto generatedDocumentAccountFromAmortization = saveNewDocumentAccountGeneratedFromAmortization(
                        currentFiscalYearId, journalId, currentFiscalYear, documentAccountLines,
                        NOT_DETAILED_DOCUMENT_ACCOUNT_LABEL_OF_AMORTIZATION);

                return generatedDocumentAccountFromAmortization;

        }

        private DocumentAccountingDto saveNewDocumentAccountGeneratedFromAmortization(Long currentFiscalYearId,
                Long journalId, FiscalYearDto currentFiscalYear, List<DocumentAccountLineDto> documentAccountLines,
                String notDetailedDocumentAccountLabelOfAmortization) {
                DocumentAccountingDto generatedDocumentAccountFromAmortization = new DocumentAccountingDto(
                        currentFiscalYear.getEndDate(),
                        String.format(notDetailedDocumentAccountLabelOfAmortization, currentFiscalYear.getName()),
                        journalId, documentAccountLines, currentFiscalYearId);

                generatedDocumentAccountFromAmortization
                        .setIndexOfStatus(DocumentAccountStatus.BY_GENERATION_FROM_AMORTIZAION_IS_CREATED.getIndex());

                saveDocumentAccount(generatedDocumentAccountFromAmortization, false);
                return generatedDocumentAccountFromAmortization;
        }

        private void throwExceptionIfNoAssetIsActiveInCurrentFiscalYear(
                List<DocumentAccountLineDto> documentAccountLines, BigDecimal totalDebitAmount,
                FiscalYearDto currentFiscalYear) {
                if (documentAccountLines.isEmpty() || totalDebitAmount.compareTo(BigDecimal.ZERO) == 0) {
                        throw new HttpCustomException(ApiErrors.Accounting.NO_ACTIVE_ASSET_IN_CURRENT_FISCAL_YEAR,
                                new ErrorsResponse().error(currentFiscalYear));
                }
        }

        private Long generateIdAmortizationAccountFromIdImmobilizationAccount(Long idImmobilizationAccountOfCategory) {

                int planCode = accountService.findById(idImmobilizationAccountOfCategory).getPlanCode();

                int amortizationAccountCode = depreciationAssetsService.generateProposedAmortizationAccount(planCode);

                return accountService.findAccountByCode(amortizationAccountCode).orElseThrow(
                        () -> new HttpCustomException(ApiErrors.Accounting.NO_ACCOUNT_WITH_CODE,
                                new ErrorsResponse().error(amortizationAccountCode))).getId();
        }

        private DocumentAccountLineDto generateDocumentAccountLine(List<DepreciationAssetsDto> depreciationAssets,
                FiscalYearDto currentFiscalYear, Long idAmortizationAccountCategory,
                Long idImmobilizationAccountAsset) {

                AccountDto immobilizationAccountAsset = accountService.findById(idImmobilizationAccountAsset);

                BigDecimal creditAmount = depreciationAssets.stream()
                        .map((DepreciationAssetsDto depreciationAssetsDto) -> amortizationTableService
                                .getDepreciationOfAsset(depreciationAssetsDto, currentFiscalYear,
                                        depreciationAssetsDto.getIdCategory()).getAnnuityExercise())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                return new DocumentAccountLineDto(currentFiscalYear.getEndDate(),
                        String.format(NOT_DETAILED_DOCUMENT_ACCOUNT_LABEL_OF_AMORTIZATION, currentFiscalYear.getName()),
                        immobilizationAccountAsset.getLabel(), BigDecimal.ZERO, creditAmount,
                        idAmortizationAccountCategory);
        }

        @Override
        public boolean isDocumentAccountGeneratedFromAmortization(Long fiscalYearId) {
                List<Long> listOfGeneratedDocumentAccountIdFromAmortization = documentAccountDao
                        .getListOfDocumentAccountGeneratedFromAmortization(fiscalYearId);
                if (listOfGeneratedDocumentAccountIdFromAmortization.isEmpty()) {
                        throw new HttpCustomException(
                                ApiErrors.Accounting.DOCUMENT_ACCOUNT_FROM_AMORTIZATION_NOT_GENERATED_YET,
                                new ErrorsResponse());
                } else {
                        return true;
                }
        }
        public List<DocumentAccountLineDto> getFiltredDALines(List<DocumentAccountLineDto> documentAccountLines) {
                List<DocumentAccountLineDto> documentAccountList = documentAccountLines.stream()
                        .filter(documentLine -> documentLine.getCreditAmount().compareTo(BigDecimal.ZERO) > 0 || documentLine
                                .getDebitAmount().compareTo(BigDecimal.ZERO) > 0).collect(Collectors.toList());
                BigDecimal totalDebitAmount = BigDecimal.ZERO;
                for (DocumentAccountLineDto documentAccountLine : documentAccountList) {
                        totalDebitAmount = totalDebitAmount.add(documentAccountLine.getDebitAmount());
                }
                BigDecimal totalCreditAmount = BigDecimal.ZERO;
                for (DocumentAccountLineDto documentAccountLine : documentAccountList) {
                        totalCreditAmount = totalCreditAmount.add(documentAccountLine.getCreditAmount());
                }

                if (!documentAccountList.isEmpty() && totalDebitAmount.compareTo(totalCreditAmount) == 0) {
                        return documentAccountList;
                }

                return new ArrayList<>();
        }
}
