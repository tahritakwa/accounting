package fr.sparkit.accounting.services.impl;

import static fr.sparkit.accounting.constants.AccountingConstants.LOG_ENTITY_CREATED;
import static fr.sparkit.accounting.constants.AccountingConstants.THOUSANDS_SEPARATOR;
import static fr.sparkit.accounting.services.impl.AccountService.MAX_ACCOUNT_CODE_LENGTH;
import static fr.sparkit.accounting.services.utils.excel.ExcelCellStyleHelper.setInvalidCell;
import static fr.sparkit.accounting.services.utils.excel.GenericExcelPOIHelper.isLabelInCellValid;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import fr.sparkit.accounting.auditing.DocumentAccountExcelCell;
import fr.sparkit.accounting.constants.LanguageConstants;
import fr.sparkit.accounting.constants.NumberConstant;
import fr.sparkit.accounting.constants.XLSXErrors;
import fr.sparkit.accounting.convertor.DocumentAccountingLineConvertor;
import fr.sparkit.accounting.dao.DocumentAccountLineDao;
import fr.sparkit.accounting.dto.AuxiliaryJournalDetailsDto;
import fr.sparkit.accounting.dto.AuxiliaryJournalDto;
import fr.sparkit.accounting.dto.CentralizingJournalDetailsByMonthDto;
import fr.sparkit.accounting.dto.CloseDocumentAccountLineDto;
import fr.sparkit.accounting.dto.DocumentAccountLineDto;
import fr.sparkit.accounting.dto.ReconciliationDocumentAccountLinePageDto;
import fr.sparkit.accounting.entities.DocumentAccount;
import fr.sparkit.accounting.entities.DocumentAccountLine;
import fr.sparkit.accounting.entities.Journal;
import fr.sparkit.accounting.services.IAccountService;
import fr.sparkit.accounting.services.IDocumentAccountLineService;
import fr.sparkit.accounting.services.IDocumentAccountService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DocumentAccountLineService extends GenericService<DocumentAccountLine, Long>
        implements IDocumentAccountLineService {

    private static final DataFormatter dataFormatter = new DataFormatter();
    private final DocumentAccountLineDao documentAccountLineDao;
    private final IAccountService accountService;
    private final IDocumentAccountService documentAccountService;

    @Autowired
    public DocumentAccountLineService(DocumentAccountLineDao documentAccountLineDao, IAccountService accountService,
            @Lazy IDocumentAccountService documentAccountService) {
        super();
        this.documentAccountLineDao = documentAccountLineDao;
        this.accountService = accountService;
        this.documentAccountService = documentAccountService;
    }

    @Override
    public void save(List<DocumentAccountLineDto> documentAccountLines, DocumentAccount documentAccount) {
        List<DocumentAccountLine> documentLineModels = new ArrayList<>();
        documentAccountLines.forEach((DocumentAccountLineDto documentAccountLineDto) -> {
            DocumentAccountLine documentAccountLine = DocumentAccountingLineConvertor
                    .dtoToModel(documentAccountLineDto);
            documentAccountLine.setId(documentAccountLineDto.getId());
            documentAccountLine.setAccount(accountService.findOne(documentAccountLineDto.getAccountId()));
            documentAccountLine.setDocumentAccount(documentAccount);
            documentLineModels.add(documentAccountLine);
            log.info(LOG_ENTITY_CREATED, documentAccountLine);
        });
        saveDocumentAccountLines(documentLineModels);
    }

    @Override
    public List<DocumentAccountLine> saveDocumentAccountLines(List<DocumentAccountLine> documentAccountLines) {
        return save(documentAccountLines);
    }

    @Override
    public List<DocumentAccountLine> findByLetterIsNull() {
        return documentAccountLineDao.findByLetterIsNullAndIsDeletedFalse();
    }

    @Override
    public String findLastLetteringCode() {
        DocumentAccountLine documentAcountLine = documentAccountLineDao
                .findFirstLetterByLetterNotNullAndIsDeletedFalseOrderByLetterDesc();
        if (documentAcountLine != null) {
            return documentAcountLine.getLetter();
        }
        return null;
    }

    @Override
    public List<DocumentAccountLine> findByLetterIsNullWithDebit() {
        return documentAccountLineDao.findByLetterIsNullWithDebit();
    }

    @Override
    public List<DocumentAccountLine> findByAccountIdAndDebitAmountAndLetterIsNullAndIsDeletedFalse(Long accountId,
            BigDecimal debitAmount) {
        return documentAccountLineDao.findByAccountIdAndDebitAmountAndLetterIsNullAndIsDeletedFalse(accountId,
                debitAmount);
    }

    @Override
    public List<DocumentAccountLine> findByDocumentAccountId(Long accountId) {
        return documentAccountLineDao.findByDocumentAccountIdAndIsDeletedFalseOrderByIdDesc(accountId);
    }

    @Override
    public boolean isLineMonetaryValuesValid(BigDecimal debit, BigDecimal credit) {
        return credit.min(debit).compareTo(BigDecimal.ZERO) == 0 && credit.max(debit).compareTo(BigDecimal.ZERO) > 0;
    }

    @Override
    public boolean isDocumentAccountLineValuesAddedToRow(DocumentAccountLineDto documentAccountLine, Row row,
            List<Field> excelHeaderFields, List<String> acceptedHeaders) {
        boolean isValid = true;
        for (int i = 0; i < excelHeaderFields.size(); i++) {
            Cell cell = row.getCell(i);
            if (cell == null) {
                cell = row.createCell(i);
            }
            if (!excelHeaderFields.get(i).getAnnotation(DocumentAccountExcelCell.class).isDocumentAccountField()) {
                switch (excelHeaderFields.get(i).getAnnotation(DocumentAccountExcelCell.class).headerName()) {
                case LanguageConstants.XLSXHeaders.ACCOUNT_CODE_HEADER_NAME:
                    isValid &= isAccountCodeSet(cell, documentAccountLine);
                    break;
                case LanguageConstants.XLSXHeaders.REFERENCE_HEADER_NAME:
                    documentAccountLine.setReference(dataFormatter.formatCellValue(cell).trim());
                    break;
                case LanguageConstants.XLSXHeaders.LINE_LABEL_HEADER_NAME:
                    isValid &= isDocumentAccountLineLabelSet(cell, documentAccountLine);
                    break;
                case LanguageConstants.XLSXHeaders.DEBIT_HEADER_NAME:
                    isValid &= isDocumentAccountLineDebitSet(cell, documentAccountLine);
                    break;
                case LanguageConstants.XLSXHeaders.CREDIT_HEADER_NAME:
                    isValid &= isDocumentAccountLineCreditSet(cell, documentAccountLine);
                    break;
                case LanguageConstants.XLSXHeaders.LINE_DATE:
                    isValid &= isDocumentAccountLineDateSet(cell, documentAccountLine);
                    break;
                default:
                    isValid = false;
                }
            }
        }
        return isValid;
    }

    private boolean isDocumentAccountLineDateSet(Cell cell, DocumentAccountLineDto documentAccountLine) {
        if (documentAccountService.isDocumentDateInCellValid(cell)) {
            LocalDateTime documentDate = LocalDateTime.ofInstant(cell.getDateCellValue().toInstant(),
                    ZoneId.systemDefault());
            documentAccountLine.setDocumentLineDate(documentDate);
            return true;
        }
        return false;
    }

    private boolean isDocumentAccountLineDebitSet(Cell cell, DocumentAccountLineDto documentAccountLine) {
        if (isMonetaryValueInCellValid(cell)) {
            BigDecimal debit = BigDecimal.valueOf(Double.parseDouble(dataFormatter.formatCellValue(cell).trim()
                    .replace(String.valueOf(THOUSANDS_SEPARATOR), StringUtils.EMPTY)));
            documentAccountLine.setDebitAmount(debit);
            return true;
        }
        return false;
    }

    private boolean isDocumentAccountLineCreditSet(Cell cell, DocumentAccountLineDto documentAccountLine) {
        if (isMonetaryValueInCellValid(cell)) {
            BigDecimal debit = BigDecimal.valueOf(Double.parseDouble(dataFormatter.formatCellValue(cell).trim()
                    .replace(String.valueOf(THOUSANDS_SEPARATOR), StringUtils.EMPTY)));
            documentAccountLine.setCreditAmount(debit);
            return true;
        }
        return false;
    }

    public boolean isDocumentAccountLineLabelSet(Cell cell, DocumentAccountLineDto documentAccountLine) {
        if (isLabelInCellValid(cell)) {
            documentAccountLine.setLabel(dataFormatter.formatCellValue(cell).trim());
            return true;
        }
        return false;
    }

    private boolean isAccountCodeSet(Cell cell, DocumentAccountLineDto documentAccountLine) {
        if (isAccountCodeInCellValid(cell)) {
            String cellValue = dataFormatter.formatCellValue(cell).trim();
            documentAccountLine.setAccountId(accountService.findByCode(Integer.parseInt(cellValue)).getId());
            return true;
        }
        return false;
    }

    @Override
    public boolean isMonetaryValueInCellValid(Cell cell) {
        if (!dataFormatter.formatCellValue(cell).trim().isEmpty()) {
            try {
                BigDecimal debit = BigDecimal.valueOf(Double.parseDouble(dataFormatter.formatCellValue(cell).trim()
                        .replace(String.valueOf(THOUSANDS_SEPARATOR), StringUtils.EMPTY)));
                if (!documentAccountService.isMonetaryValueNegativeOrScaleInvalid(cell, debit)) {
                    return true;
                }
            } catch (NumberFormatException e) {
                setInvalidCell(cell, XLSXErrors.DocumentAccountXLSXErrors.MONETARY_CELL_SHOULD_BE_OF_TYPE_NUMBER);
            }
        } else {
            setInvalidCell(cell, XLSXErrors.REQUIRED_FIELD);
        }
        return false;
    }

    @Override
    public List<DocumentAccountLine> findReconcilableLinesUsingJournal(Long currentFiscalYearId, Long journalId) {
        return documentAccountLineDao.findReconcilableLinesUsingJournal(currentFiscalYearId, journalId);
    }

    private boolean isAccountCodeInCellValid(Cell cell) {
        String cellValue = dataFormatter.formatCellValue(cell).trim();
        if (!cellValue.isEmpty()) {
            try {
                int accountCode = Integer.parseInt(cellValue);
                if (accountCode < 0 || cellValue.length() != MAX_ACCOUNT_CODE_LENGTH) {
                    setInvalidCell(cell, String.format(XLSXErrors.AccountXLSXErrors.ACCOUNT_CODE_INVALID_FORMAT,
                            MAX_ACCOUNT_CODE_LENGTH));
                } else {
                    if (accountService.isAccountCodeUsedInMultipleAccounts(accountCode)) {
                        setInvalidCell(cell,
                                String.format(XLSXErrors.DUPLICATE_ACCOUNTS_FOUND_WITH_THE_SAME_CODE, accountCode));
                    } else if (!accountService.findAccountByCode(accountCode).isPresent()) {
                        setInvalidCell(cell, String.format(XLSXErrors.NO_ACCOUNT_WITH_CODE, accountCode));
                    } else {
                        return true;
                    }
                }
                return false;
            } catch (NumberFormatException e) {
                setInvalidCell(cell, XLSXErrors.ACCOUNT_ACCOUNT_CODE_CELL_SHOULD_BE_OF_TYPE_NUMBER);
            }
        } else {
            setInvalidCell(cell, XLSXErrors.REQUIRED_FIELD);
        }
        return false;
    }

    @Override
    public List<DocumentAccountLine> findLinesWithNoLetterForBalancedAccountsInFiscalYear(LocalDateTime startDate,
            LocalDateTime endDate) {
        return documentAccountLineDao.findLinesWithNoLetterForBalancedAccountsInFiscalYear(startDate, endDate);
    }

    @Override
    public List<DocumentAccountLine> findLinesWithNoLetterForRevenueAndExpensesAccountsInFiscalYear(
            LocalDateTime startDate, LocalDateTime endDate) {
        return documentAccountLineDao.findLinesWithNoLetterForRevenueAndExpensesAccountsInFiscalYear(startDate,
                endDate);
    }

    @Override
    public List<DocumentAccountLine> getNotLetteredLines() {
        return documentAccountLineDao.getNotLetteredLines();
    }

    @Override
    public List<DocumentAccountLine> getLetteredLines() {
        return documentAccountLineDao.getLetteredLines();
    }

    @Override
    public List<CloseDocumentAccountLineDto> getReconcilableDocumentAccountLineInBetween(Long accountId,
            LocalDateTime startDate, LocalDateTime endDate) {
        List<DocumentAccountLine> filteredReconcilableDocumentAccountLines = documentAccountLineDao
                .getReconcilableDocumentAccountLineNotCloseInBetween(accountId, startDate, endDate);
        filteredReconcilableDocumentAccountLines.sort(getDefaultReconciliationBankSort());
        List<CloseDocumentAccountLineDto> documentAccountLines = DocumentAccountingLineConvertor
                .documentAccountLinesToCloseDocumentAccountLineDtos(filteredReconcilableDocumentAccountLines);
        addTotalToClosedDocumentAccountLinesList(documentAccountLines);
        return documentAccountLines;
    }

    @Override
    public ReconciliationDocumentAccountLinePageDto getReconcilableDocumentAccountLineInBetween(Long accountId,
            LocalDateTime startDate, LocalDateTime endDate,
            List<CloseDocumentAccountLineDto> documentAccountLineAffected, Pageable pageable) {
        List<DocumentAccountLine> documentAccountLines;
        if (documentAccountLineAffected.isEmpty()) {
            documentAccountLines = documentAccountLineDao
                    .getInitialReconcilableDocumentAccountLineNotCloseInBetween(accountId, startDate, endDate);
        } else {
            List<Long> documentAccountLineAffectedId = documentAccountLineAffected.stream()
                    .map(CloseDocumentAccountLineDto::getId).collect(Collectors.toList());
            documentAccountLines = documentAccountLineDao.getReconcilableDocumentAccountLineNotCloseInBetween(accountId,
                    startDate, endDate, documentAccountLineAffectedId);
        }
        sortReconciliationLines(documentAccountLines, pageable);
        int beginIndex = Math.toIntExact(pageable.getOffset());
        int endIndex = beginIndex + (Math.toIntExact(pageable.getPageSize())
                - (documentAccountLineAffected.size() % Math.toIntExact(pageable.getPageSize())));
        if (endIndex > documentAccountLines.size()) {
            endIndex = documentAccountLines.size();
        }
        List<DocumentAccountLine> documentAccountLinesReturn = new ArrayList<>();
        if (!documentAccountLines.isEmpty() && beginIndex < endIndex) {
            documentAccountLinesReturn = documentAccountLines.subList(beginIndex, endIndex);
        }
        BigDecimal totalDebit = documentAccountLines.stream().map(DocumentAccountLine::getDebitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = documentAccountLines.stream().map(DocumentAccountLine::getCreditAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ReconciliationDocumentAccountLinePageDto(DocumentAccountingLineConvertor
                .documentAccountLinesToCloseDocumentAccountLineDtos(documentAccountLinesReturn),
                (long) documentAccountLines.size(), totalDebit, totalCredit);
    }

    private Comparator<DocumentAccountLine> getDefaultReconciliationBankSort() {
        return Comparator
                .comparing((DocumentAccountLine documentAccountLine) -> documentAccountLine.getDocumentLineDate()
                        .toLocalDate(), Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing((DocumentAccountLine documentAccountLine) -> documentAccountLine.getDocumentAccount()
                        .getCodeDocument(), Comparator.nullsFirst(Comparator.naturalOrder()));
    }

    private void sortReconciliationLines(List<DocumentAccountLine> documentAccountLines, Pageable pageable) {
        Comparator<DocumentAccountLine> defaultSort = getDefaultReconciliationBankSort();
        Comparator<DocumentAccountLine> sort;
        Optional<Sort.Order> sortOrder = pageable.getSort().get().findFirst();
        Sort.Direction direction;
        if (!sortOrder.isPresent()) {
            sort = defaultSort;
            direction = Sort.Direction.ASC;
        } else {
            direction = sortOrder.get().getDirection();
            switch (sortOrder.get().getProperty()) {
            case "codeDocument":
                sort = Comparator.comparing((DocumentAccountLine documentAccountLine) -> documentAccountLine
                        .getDocumentAccount().getCodeDocument(), Comparator.nullsFirst(Comparator.naturalOrder()));
                break;
            case "journalLabel":
                sort = Comparator.comparing((DocumentAccountLine documentAccountLine) -> documentAccountLine
                        .getDocumentAccount().getJournal().getLabel(),
                        Comparator.nullsFirst(Comparator.naturalOrder()));
                break;
            case "documentDate":
                sort = defaultSort;
                break;
            case "reference":
                sort = Comparator.comparing(DocumentAccountLine::getReference,
                        Comparator.nullsFirst(Comparator.naturalOrder()));
                break;
            case "label":
                sort = Comparator.comparing(DocumentAccountLine::getLabel,
                        Comparator.nullsFirst(Comparator.naturalOrder()));
                break;
            case "debitAmount":
                sort = Comparator.comparing(DocumentAccountLine::getDebitAmount,
                        Comparator.nullsFirst(Comparator.naturalOrder()));
                break;
            case "creditAmount":
                sort = Comparator.comparing(DocumentAccountLine::getCreditAmount,
                        Comparator.nullsFirst(Comparator.naturalOrder()));
                break;
            default:
                sort = defaultSort;
            }
        }
        documentAccountLines.sort(sort);
        if (Sort.Direction.DESC.equals(direction)) {
            Collections.reverse(documentAccountLines);
        }
    }

    @Override
    public List<CloseDocumentAccountLineDto> getCloseDocumentAccountLineInBetween(Long accountId, LocalDate startDate,
            LocalDate endDate) {
        List<DocumentAccountLine> filteredCloseDocumentAccountLines = documentAccountLineDao
                .getCloseDocumentAccountLineInBetween(accountId, startDate, endDate.plusDays(NumberConstant.ONE));
        filteredCloseDocumentAccountLines.sort(getDefaultReconciliationBankSort());
        return DocumentAccountingLineConvertor
                .documentAccountLinesToCloseDocumentAccountLineDtos(filteredCloseDocumentAccountLines);
    }

    @Override
    public List<DocumentAccountLine> getCloseDocumentAccountLineInBetweenDate(Long accountId, LocalDate startDate,
            LocalDate endDate, List<Long> documentAccountLineReleased, Pageable pageable) {
        List<DocumentAccountLine> filteredCloseDocumentAccountLines;
        if (documentAccountLineReleased.isEmpty()) {
            filteredCloseDocumentAccountLines = documentAccountLineDao.getInitialCloseDocumentAccountLineInBetween(
                    accountId, startDate, endDate.plusDays(NumberConstant.ONE));
        } else {
            filteredCloseDocumentAccountLines = documentAccountLineDao.getCloseDocumentAccountLineInBetween(accountId,
                    startDate, endDate.plusDays(NumberConstant.ONE), documentAccountLineReleased);
        }
        sortReconciliationLines(filteredCloseDocumentAccountLines, pageable);
        return filteredCloseDocumentAccountLines;
    }

    @Override
    public List<DocumentAccountLine> getCloseDocumentAccountLineInBetweenDate(Long accountId, LocalDate startDate,
            LocalDate endDate, List<Long> documentAccountLineReleased) {
        List<DocumentAccountLine> filteredCloseDocumentAccountLines;
        if (documentAccountLineReleased.isEmpty()) {
            filteredCloseDocumentAccountLines = documentAccountLineDao.getInitialCloseDocumentAccountLineInBetween(
                    accountId, startDate, endDate.plusDays(NumberConstant.ONE));
        } else {
            filteredCloseDocumentAccountLines = documentAccountLineDao.getCloseDocumentAccountLineInBetween(accountId,
                    startDate, endDate.plusDays(NumberConstant.ONE), documentAccountLineReleased);
        }
        return filteredCloseDocumentAccountLines;
    }

    @Override
    public void setDocumentAccountLineIsClose(List<Long> ids, LocalDate reconciliationDate) {
        documentAccountLineDao.setDocumentAccountLineIsClose(ids, reconciliationDate);
    }

    @Override
    public void setDocumentaccountLineIsNotClose(List<Long> ids) {
        documentAccountLineDao.setDocumentAccountLineIsNotClose(ids);
    }

    @Override
    public Optional<BigDecimal> totalDebitAmountDocumentAccount(Long documentAccountId) {
        return documentAccountLineDao.totalDebitAmountDocumentAccount(documentAccountId);
    }

    @Override
    public List<DocumentAccountLine> getDocumentLineDtosByDocumentIds(List<Long> documentIds) {
        List<List<Long>> idsPartitioned = ListUtils.partition(documentIds, SQL_SERVER_IN_CLAUSE_PARTITION_SIZE);
        List<DocumentAccountLine> documentAccountLines = new ArrayList<>();
        idsPartitioned.forEach((List<Long> entitiesPartition) -> documentAccountLines
                .addAll(documentAccountLineDao.getDocumentLineDtosByDocumentIds(entitiesPartition)));
        return documentAccountLines;
    }

    @Override
    public Optional<BigDecimal> totalCreditAmountDocumentAccount(Long documentAccountId) {
        return documentAccountLineDao.totalCreditAmountDocumentAccount(documentAccountId);
    }

    @Override
    public Long getTotalReconcilableLinesByDocumentAccountId(Long id) {
        return documentAccountLineDao.getTotalReconcilableLinesByDocumentAccountId(id);
    }

    @Override
    public Long getCountLetteredLinesByDocumentAccountId(Long id) {
        return documentAccountLineDao.getCountLetteredLinesByDocumentAccountId(id);
    }

    @Override
    public List<CentralizingJournalDetailsByMonthDto> getCentralizingJournalDetailsDto(Long journalId,
            LocalDateTime startDate, LocalDateTime endDate, int breakingAccount, String customerAccountCode,
            String supplierAccountCode) {
        return documentAccountLineDao.getCentralizingJournalDetailsDto(journalId, startDate, endDate, breakingAccount,
                customerAccountCode, supplierAccountCode);
    }

    @Override
    public List<CentralizingJournalDetailsByMonthDto> getCentralizingJournalTiersDetailsDto(Long journalId,
            LocalDateTime startDate, LocalDateTime endDate, int breakingTiersAccount, String tierAccountCode) {
        return documentAccountLineDao.getCentralizingJournalTiersDetailsDto(journalId, startDate, endDate,
                breakingTiersAccount, tierAccountCode);
    }

    @Override
    public List<DocumentAccountLine> findDocumentAccountLineByJournalAndDocumentDateBetween(Long journalId,
            LocalDateTime startDate, LocalDateTime endDate) {
        return documentAccountLineDao.findDocumentAccountLineByJournalAndDocumentDateBetween(journalId, startDate,
                endDate);
    }

    @Override
    public List<AuxiliaryJournalDto> getAuxiliaryJournalDtos(LocalDateTime startDate, LocalDateTime endDate,
            List<Journal> journals) {
        return journals.stream()
                .map((Journal journal) -> documentAccountLineDao
                        .findAuxiliaryJournalDto(startDate, endDate, journal.getId())
                        .orElse(new AuxiliaryJournalDto(journal.getId(), journal.getCode(), journal.getLabel(),
                                BigDecimal.ZERO, BigDecimal.ZERO)))
                .collect(Collectors.toList());
    }

    @Override
    public Page<AuxiliaryJournalDetailsDto> getByIdAuxiliaryJournalDetailsPage(Long id, LocalDateTime startDate,
            LocalDateTime endDate, Pageable pageable) {
        List<AuxiliaryJournalDetailsDto> auxiliaryJournalDetails = getByIdAuxiliaryJournalDetails(id, startDate,
                endDate);
        sortAuxiliaryJournalDetails(auxiliaryJournalDetails, pageable);
        int beginIndex = Math.toIntExact(pageable.getOffset());
        int endIndex = beginIndex + Math.toIntExact(pageable.getPageSize());
        if (beginIndex + pageable.getPageSize() > auxiliaryJournalDetails.size()) {
            endIndex = auxiliaryJournalDetails.size();
        }
        List<AuxiliaryJournalDetailsDto> AuxiliaryJournalDetailsDto = new ArrayList<>(
                auxiliaryJournalDetails.subList(beginIndex, endIndex));
        return new PageImpl<>(AuxiliaryJournalDetailsDto, pageable, auxiliaryJournalDetails.size());
    }

    private Comparator<AuxiliaryJournalDetailsDto> getDefaultAuxiliaryJournalSort() {
        return Comparator
                .comparing((AuxiliaryJournalDetailsDto auxiliaryJournalDetail) -> auxiliaryJournalDetail.getDate()
                        .toLocalDate(), Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(AuxiliaryJournalDetailsDto::getFolio, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(AuxiliaryJournalDetailsDto::getAccountCode,
                        Comparator.nullsFirst(Comparator.naturalOrder()));
    }

    private void sortAuxiliaryJournalDetails(List<AuxiliaryJournalDetailsDto> auxiliaryJournalDetails,
            Pageable pageable) {
        Comparator<AuxiliaryJournalDetailsDto> defaultSort = getDefaultAuxiliaryJournalSort();
        Comparator<AuxiliaryJournalDetailsDto> sort;
        Optional<Sort.Order> sortOrder = pageable.getSort().get().findFirst();
        Sort.Direction direction;
        if (!sortOrder.isPresent()) {
            sort = defaultSort;
            direction = Sort.Direction.ASC;
        } else {
            direction = sortOrder.get().getDirection();
            switch (sortOrder.get().getProperty()) {
            case "documentAccountDate":
                sort = defaultSort;
                break;
            case "folio":
                sort = Comparator.comparing(AuxiliaryJournalDetailsDto::getFolio,
                        Comparator.nullsFirst(Comparator.naturalOrder()));
                break;
            case "documentAccountDateLine":
                sort = Comparator.comparing(AuxiliaryJournalDetailsDto::getDocumentLineDate,
                        Comparator.nullsFirst(Comparator.naturalOrder()));
                break;
            case "accountCode":
                sort = Comparator.comparing(AuxiliaryJournalDetailsDto::getAccountCode,
                        Comparator.nullsFirst(Comparator.naturalOrder()));
                break;
            case "label":
                sort = Comparator.comparing(AuxiliaryJournalDetailsDto::getLabel,
                        Comparator.nullsFirst(Comparator.naturalOrder()));
                break;
            case "credit":
                sort = Comparator.comparing(AuxiliaryJournalDetailsDto::getCredit,
                        Comparator.nullsFirst(Comparator.naturalOrder()));
                break;
            case "debit":
                sort = Comparator.comparing(AuxiliaryJournalDetailsDto::getDebit,
                        Comparator.nullsFirst(Comparator.naturalOrder()));
                break;
            default:
                sort = defaultSort;
            }
        }
        auxiliaryJournalDetails.sort(sort);
        if (Sort.Direction.DESC.equals(direction)) {
            Collections.reverse(auxiliaryJournalDetails);
        }
    }

    @Override
    public List<AuxiliaryJournalDetailsDto> getByIdAuxiliaryJournalDetails(Long id, LocalDateTime startDate,
            LocalDateTime endDate) {
        List<DocumentAccountLine> documentAccountLines = documentAccountLineDao.findAuxiliaryJournalDetails(id,
                startDate, endDate);
        documentAccountLines.sort(Comparator
                .comparing((DocumentAccountLine documentAccountLine) -> documentAccountLine.getDocumentAccount()
                        .getDocumentDate().toLocalDate())
                .thenComparing((DocumentAccountLine documentAccountLine) -> documentAccountLine.getDocumentAccount()
                        .getCodeDocument())
                .thenComparing(
                        (DocumentAccountLine documentAccountLine) -> documentAccountLine.getAccount().getCode()));

        List<AuxiliaryJournalDetailsDto> auxiliaryJournalDetails = new ArrayList<>();
        documentAccountLines.forEach(documentAccountLine -> {
            auxiliaryJournalDetails.add(new AuxiliaryJournalDetailsDto(documentAccountLine.getId(),
                    documentAccountLine.getDocumentAccount().getDocumentDate(),
                    documentAccountLine.getDocumentAccount().getCodeDocument(),
                    documentAccountLine.getDocumentLineDate(), documentAccountLine.getAccount().getCode(),
                    documentAccountLine.getLabel(), documentAccountLine.getDebitAmount(),
                    documentAccountLine.getCreditAmount(), documentAccountLine.getDocumentAccount().getId()));
        });
        return auxiliaryJournalDetails;
    }

    @Override
    public void addTotalToClosedDocumentAccountLinesList(List<CloseDocumentAccountLineDto> closeDocumentAccountLines) {
        BigDecimal sumDebit = closeDocumentAccountLines.stream().map(CloseDocumentAccountLineDto::getDebitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sumCredit = closeDocumentAccountLines.stream().map(CloseDocumentAccountLineDto::getCreditAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        closeDocumentAccountLines.add(new CloseDocumentAccountLineDto(LanguageConstants.TOTAL, sumDebit, sumCredit));
    }

    @Override
    public List<CloseDocumentAccountLineDto> getAllReconcilableDocumentAccountLineInBetween(Long accountId,
            LocalDateTime startDate, LocalDateTime endDate,
            List<CloseDocumentAccountLineDto> documentAccountLineAffected) {
        List<DocumentAccountLine> documentAccountLines;
        if (documentAccountLineAffected.isEmpty()) {
            documentAccountLines = documentAccountLineDao
                    .getInitialReconcilableDocumentAccountLineNotCloseInBetween(accountId, startDate, endDate);
        } else {
            List<Long> documentAccountLineAffectedId = documentAccountLineAffected.stream()
                    .map(CloseDocumentAccountLineDto::getId).collect(Collectors.toList());
            documentAccountLines = documentAccountLineDao.getReconcilableDocumentAccountLineNotCloseInBetween(accountId,
                    startDate, endDate, documentAccountLineAffectedId);
        }
        return DocumentAccountingLineConvertor.documentAccountLinesToCloseDocumentAccountLineDtos(documentAccountLines);
    }

}
