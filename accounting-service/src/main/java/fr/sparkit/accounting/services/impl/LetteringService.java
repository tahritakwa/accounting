package fr.sparkit.accounting.services.impl;

import static fr.sparkit.accounting.constants.AccountingConstants.BEGIN_ACCOUNT_MUST_NOT_GRATER_THAN_END_ACCOUNT;
import static fr.sparkit.accounting.constants.AccountingConstants.DATES_ORDER_INVALID;
import static fr.sparkit.accounting.constants.AccountingConstants.TOTAL_DEBIT_SHOULD_BE_EQUAL_TO_TOTAL_CREDIT_FOR_ACCOUNT_AND_LETTER;
import static fr.sparkit.accounting.constants.AccountingConstants.YOU_MUST_CHOOSE_FOR_THE_THE_SAME_LETTER_SINGLE_ACCOUNT;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.stereotype.Service;

import fr.sparkit.accounting.constants.AccountingConstants;
import fr.sparkit.accounting.constants.NumberConstant;
import fr.sparkit.accounting.dao.DocumentAccountLineDao;
import fr.sparkit.accounting.dto.DocumentAccountingDto;
import fr.sparkit.accounting.dto.LiterableDocumentAccountLineDto;
import fr.sparkit.accounting.dto.LiterableDocumentAccountLinePageDto;
import fr.sparkit.accounting.entities.DocumentAccountLine;
import fr.sparkit.accounting.services.IAccountingConfigurationService;
import fr.sparkit.accounting.services.IDocumentAccountLineService;
import fr.sparkit.accounting.services.IDocumentAccountService;
import fr.sparkit.accounting.services.IFiscalYearService;
import fr.sparkit.accounting.services.ILetteringService;
import fr.sparkit.accounting.services.utils.AccountingServiceUtil;
import fr.sparkit.accounting.services.utils.LettringUtil;
import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.errors.ErrorsResponse;
import fr.sparkit.accounting.util.http.HttpCustomException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LetteringService implements ILetteringService {

    private final DocumentAccountLineDao documentAccountLineDao;
    private final IDocumentAccountService documentAccountService;
    private final IFiscalYearService fiscalYearService;
    private final IAccountingConfigurationService accountingConfigurationService;
    private final IDocumentAccountLineService documentAccountLineService;

    @Autowired
    public LetteringService(DocumentAccountLineDao documentAccountLineDao,
            IDocumentAccountService documentAccountService, IFiscalYearService fiscalYearService,
            IAccountingConfigurationService accountingConfigurationService,
            IDocumentAccountLineService documentAccountLineService) {
        this.documentAccountLineDao = documentAccountLineDao;
        this.documentAccountService = documentAccountService;
        this.fiscalYearService = fiscalYearService;
        this.accountingConfigurationService = accountingConfigurationService;
        this.documentAccountLineService = documentAccountLineService;
    }

    @Override
    public LiterableDocumentAccountLinePageDto findDocumentAccountLinesForLiterableAccount(int accountPage,
            int literableLinePageSize, int literableLinePage, Boolean havingLetteredLines, String beginAccountCode,
            String endAccountCode, LocalDateTime startDate, LocalDateTime endDate, Boolean sameAmount, String field,
            String direction) {
        if (AccountingServiceUtil.isDateBeforeOrEquals(startDate, endDate)) {
            if (AccountingServiceUtil.isCodeLowerThanOrEquals(beginAccountCode, endAccountCode)) {

                Pageable pageableOfAccounts = PageRequest.of(accountPage, NumberConstant.ONE);
                Pageable pageableOfDocumentAccountLines;
                Sort sortField;
                if (Boolean.TRUE.equals(sameAmount)) {
                    pageableOfDocumentAccountLines = PageRequest.of(literableLinePage, literableLinePageSize);
                } else {
                    if (StringUtils.isNoneBlank(field) && StringUtils.isNoneBlank(direction)) {
                        sortField = getEntityFieldNameSort(field,
                                Sort.Direction.valueOf(direction.toUpperCase(AccountingConstants.LANGUAGE)));
                        pageableOfDocumentAccountLines = PageRequest.of(literableLinePage, literableLinePageSize,
                                sortField);
                    } else {
                        String[] orders = { " documentAccount.documentDate", "documentAccount.codeDocument" };
                        sortField = JpaSort.unsafe(Sort.Direction.ASC, orders);
                        pageableOfDocumentAccountLines = PageRequest.of(literableLinePage, literableLinePageSize,
                                sortField);
                    }
                }
                int parseBeginAccountCode = AccountingServiceUtil.getDefaultAccountCode(beginAccountCode,
                        AccountingConstants.MIN_ACCOUNT_CODE);
                int parseEndAccountCode = AccountingServiceUtil.getDefaultAccountCode(endAccountCode,
                        AccountingConstants.LETTERING_MAX_ACCOUNT_CODE);

                Page<Integer> accountCodePage;
                if (Boolean.TRUE.equals(havingLetteredLines)) {
                    accountCodePage = documentAccountLineDao.getLiterableAccountHavingLetteredLines(
                            parseBeginAccountCode, parseEndAccountCode, startDate, endDate, pageableOfAccounts);
                } else {
                    accountCodePage = documentAccountLineDao.getLiterableAccountHavingNotLetteredLines(
                            parseBeginAccountCode, parseEndAccountCode, startDate, endDate, pageableOfAccounts);
                }
                Page<DocumentAccountLine> documentAccountLines;
                LiterableDocumentAccountLinePageDto literableDocumentAccountLinePage = new LiterableDocumentAccountLinePageDto();
                literableDocumentAccountLinePage.setTotalDebit(BigDecimal.ZERO);
                literableDocumentAccountLinePage.setTotalCredit(BigDecimal.ZERO);
                if (!accountCodePage.getContent().isEmpty()) {
                    if (Boolean.TRUE.equals(sameAmount)) {
                        documentAccountLines = findDocumentAccountLinesByAccountCodeAndLetterSameAmount(
                                havingLetteredLines, startDate, endDate, pageableOfDocumentAccountLines,
                                accountCodePage);
                    } else {
                        documentAccountLines = findDocumentAccountLinesByAccountCodeAndLetter(havingLetteredLines,
                                startDate, endDate, pageableOfDocumentAccountLines, accountCodePage);
                    }
                    List<LiterableDocumentAccountLineDto> literableDocumentAccountLines = new ArrayList<>();
                    LettringUtil.fillLiterableLinesUsingDocumentAccountLines(literableDocumentAccountLines,
                            documentAccountLines.getContent());
                    literableDocumentAccountLinePage = new LiterableDocumentAccountLinePageDto(
                            literableDocumentAccountLines, accountCodePage.getTotalElements(),
                            documentAccountLines.getTotalElements(), BigDecimal.ZERO, BigDecimal.ZERO);
                } else {
                    literableDocumentAccountLinePage.setContent(new ArrayList<>());
                }
                return literableDocumentAccountLinePage;
            } else {
                log.error(BEGIN_ACCOUNT_MUST_NOT_GRATER_THAN_END_ACCOUNT);
                throw new HttpCustomException(ApiErrors.Accounting.BEGIN_ACCOUNT_CODE_IS_GREATER_THAN_END_ACCOUNT);
            }
        } else {
            log.error(DATES_ORDER_INVALID);
            throw new HttpCustomException(ApiErrors.Accounting.START_DATE_IS_AFTER_END_DATE);
        }
    }

    private Sort getEntityFieldNameSort(String field, Direction direction) {
        switch (field) {
        case "documentAccountDate":
            return Sort.by(new Sort.Order(direction, "documentAccount.documentDate").ignoreCase());
        case "documentAccountCode":
            return Sort.by(new Sort.Order(direction, "documentAccount.codeDocument").ignoreCase());
        case "journal":
            return Sort.by(new Sort.Order(direction, "documentAccount.journal.label").ignoreCase());
        case "debit":
            return Sort.by(new Sort.Order(direction, "debitAmount"));
        case "credit":
            return Sort.by(new Sort.Order(direction, "creditAmount"));
        default:
            break;
        }
        return Sort.by(new Sort.Order(direction, field).ignoreCase());
    }

    private Page<DocumentAccountLine> findDocumentAccountLinesByAccountCodeAndLetter(Boolean havingLetteredLines,
            LocalDateTime startDate, LocalDateTime endDate, Pageable pageableOfDocumentAccountLines,
            Page<Integer> accountCodePage) {
        if (Boolean.TRUE.equals(havingLetteredLines)) {
            return documentAccountLineDao.findByAccountCodeAndLetterIsNotNull(
                    accountCodePage.getContent().get(NumberConstant.ZERO), startDate, endDate,
                    pageableOfDocumentAccountLines);
        } else {
            return documentAccountLineDao.findByAccountCodeAndLetterIsNull(
                    accountCodePage.getContent().get(NumberConstant.ZERO), startDate, endDate,
                    pageableOfDocumentAccountLines);
        }
    }

    private Page<DocumentAccountLine> findDocumentAccountLinesByAccountCodeAndLetterSameAmount(
            Boolean havingLetteredLines, LocalDateTime startDate, LocalDateTime endDate,
            Pageable pageableOfDocumentAccountLines, Page<Integer> accountCodePage) {
        if (Boolean.TRUE.equals(havingLetteredLines)) {
            return documentAccountLineDao.findByAccountCodeAndLetterIsNotNullAndSameAmount(
                    accountCodePage.getContent().get(NumberConstant.ZERO), startDate, endDate,
                    pageableOfDocumentAccountLines);
        } else {
            return documentAccountLineDao.findByAccountCodeAndLetterIsNullAndSameAmount(
                    accountCodePage.getContent().get(NumberConstant.ZERO), startDate, endDate,
                    pageableOfDocumentAccountLines);
        }
    }

    @Override
    public LiterableDocumentAccountLinePageDto autoLiterateDocumentAccountLines(int accountPage,
            int literableLinePageSize, int literableLinePage, String beginAccountCode, String endAccountCode,
            LocalDateTime startDate, LocalDateTime endDate, String field, String direction) {
        if (AccountingServiceUtil.isDateBeforeOrEquals(startDate, endDate)) {
            if (AccountingServiceUtil.isCodeLowerThanOrEquals(beginAccountCode, endAccountCode)) {
                Pageable pageableOfAccounts = PageRequest.of(accountPage, NumberConstant.ONE);
                Pageable pageableOfDocumentAccountLines;
                Sort sortField;
                int parseBeginAccountCode = AccountingServiceUtil.getDefaultAccountCode(beginAccountCode,
                        AccountingConstants.MIN_ACCOUNT_CODE);
                int parseEndAccountCode = AccountingServiceUtil.getDefaultAccountCode(endAccountCode,
                        AccountingConstants.LETTERING_MAX_ACCOUNT_CODE);

                Page<Integer> accountCodePage = documentAccountLineDao.getLiterableAccountHavingNotLetteredLines(
                        parseBeginAccountCode, parseEndAccountCode, startDate, endDate, pageableOfAccounts);
                LiterableDocumentAccountLinePageDto literableDocumentAccountLinePage = new LiterableDocumentAccountLinePageDto();
                literableDocumentAccountLinePage.setTotalDebit(BigDecimal.ZERO);
                literableDocumentAccountLinePage.setTotalCredit(BigDecimal.ZERO);
                if (!accountCodePage.getContent().isEmpty()) {
                    if (StringUtils.isNoneBlank(field) && StringUtils.isNoneBlank(direction)) {
                        sortField = getEntityFieldNameSort(field,
                                Sort.Direction.valueOf(direction.toUpperCase(AccountingConstants.LANGUAGE)));
                        pageableOfDocumentAccountLines = PageRequest.of(literableLinePage, literableLinePageSize,
                                sortField);
                    } else {
                        String[] orders = { " documentAccount.documentDate", "documentAccount.codeDocument" };
                        sortField = JpaSort.unsafe(Sort.Direction.ASC, orders);
                        pageableOfDocumentAccountLines = PageRequest.of(literableLinePage, literableLinePageSize,
                                sortField);
                    }
                    Page<DocumentAccountLine> documentAccountLines = documentAccountLineDao
                            .findByAccountCodeAndLetterIsNull(accountCodePage.getContent().get(NumberConstant.ZERO),
                                    startDate, endDate, pageableOfDocumentAccountLines);

                    List<String> distinctReferences = documentAccountLineDao
                            .getDistinctReferenceByAccountAndLetterIsNull(
                                    accountCodePage.getContent().get(NumberConstant.ZERO), startDate, endDate);
                    List<String> distinctNotEmptyAndNotNullReferences = distinctReferences.stream()
                            .filter(reference -> reference != null && !StringUtils.EMPTY.equals(reference.trim()))
                            .collect(Collectors.toList());
                    String letter = generateFirstUnusedLetter();
                    List<LiterableDocumentAccountLineDto> literableDocumentAccountLines = new ArrayList<>();
                    for (String reference : distinctNotEmptyAndNotNullReferences) {
                        List<DocumentAccountLine> documentAccountLinesByAccountAndReferenceAndLetterIsNull = documentAccountLineDao
                                .findByAccountCodeAndReferenceAndLetterIsNull(
                                        accountCodePage.getContent().get(NumberConstant.ZERO), startDate, endDate,
                                        reference);
                        letter = LettringUtil.literateDocumentAccountLinesByAccountAndReference(
                                documentAccountLines.getContent(),
                                documentAccountLinesByAccountAndReferenceAndLetterIsNull, letter,
                                documentAccountLineDao, literableDocumentAccountLinePage);
                    }
                    LettringUtil.fillLiterableLinesUsingDocumentAccountLines(literableDocumentAccountLines,
                            documentAccountLines.getContent());
                    LettringUtil.sortLiterableDocumentAccountLinesByLetter(literableDocumentAccountLines);
                    literableDocumentAccountLinePage.setContent(literableDocumentAccountLines);
                    literableDocumentAccountLinePage.setTotalElementsOfAccounts(accountCodePage.getTotalElements());
                    literableDocumentAccountLinePage
                            .setTotalElementsOfDocumentAccountLinesPerAccount(documentAccountLines.getTotalElements());
                } else {
                    literableDocumentAccountLinePage.setContent(new ArrayList<>());
                }
                return literableDocumentAccountLinePage;
            } else {
                log.error(BEGIN_ACCOUNT_MUST_NOT_GRATER_THAN_END_ACCOUNT);
                throw new HttpCustomException(ApiErrors.Accounting.BEGIN_ACCOUNT_CODE_IS_GREATER_THAN_END_ACCOUNT);
            }
        } else {
            log.error(DATES_ORDER_INVALID);
            throw new HttpCustomException(ApiErrors.Accounting.START_DATE_IS_AFTER_END_DATE);
        }
    }

    @Override
    public synchronized LiterableDocumentAccountLinePageDto autoLiterateDocumentAccountLinesWithOrder(int accountPage,
            int literableLinePageSize, int literableLinePage, String beginAccountCode, String endAccountCode,
            LocalDateTime startDate, LocalDateTime endDate) {
        if (AccountingServiceUtil.isDateBeforeOrEquals(startDate, endDate)) {
            if (AccountingServiceUtil.isCodeLowerThanOrEquals(beginAccountCode, endAccountCode)) {
                Pageable pageableOfAccounts = PageRequest.of(accountPage, NumberConstant.ONE);
                int parseBeginAccountCode = AccountingServiceUtil.getDefaultAccountCode(beginAccountCode,
                        AccountingConstants.MIN_ACCOUNT_CODE);
                int parseEndAccountCode = AccountingServiceUtil.getDefaultAccountCode(endAccountCode,
                        AccountingConstants.LETTERING_MAX_ACCOUNT_CODE);

                Page<Integer> accountCodePage = documentAccountLineDao.getLiterableAccountHavingNotLetteredLines(
                        parseBeginAccountCode, parseEndAccountCode, startDate, endDate, pageableOfAccounts);
                LiterableDocumentAccountLinePageDto literableDocumentAccountLinePage = new LiterableDocumentAccountLinePageDto();
                literableDocumentAccountLinePage.setTotalDebit(BigDecimal.ZERO);
                literableDocumentAccountLinePage.setTotalCredit(BigDecimal.ZERO);
                if (!accountCodePage.getContent().isEmpty()) {
                    List<DocumentAccountLine> documentAccountLinesByAccountAndReferenceNotEmptyAndLetterIsNull = documentAccountLineDao
                            .findByAccountCodeAndReferenceNotEmptyAndLetterIsNull(
                                    accountCodePage.getContent().get(NumberConstant.ZERO), startDate, endDate);

                    List<String> distinctNotEmptyAndNotNullReferences = documentAccountLineDao
                            .getDistinctReferenceByAccountAndLetterIsNull(
                                    accountCodePage.getContent().get(NumberConstant.ZERO), startDate, endDate);

                    String letter = generateFirstUnusedLetter();
                    List<LiterableDocumentAccountLineDto> literableDocumentAccountLines = new ArrayList<>();
                    for (String reference : distinctNotEmptyAndNotNullReferences) {
                        List<DocumentAccountLine> documentAccountLinesByAccountAndReferenceAndLetterIsNull = documentAccountLinesByAccountAndReferenceNotEmptyAndLetterIsNull
                                .stream().filter(line -> line.getReference().equals(reference))
                                .collect(Collectors.toList());

                        letter = LettringUtil.literateDocumentAccountLinesByAccountAndReference(
                                documentAccountLinesByAccountAndReferenceNotEmptyAndLetterIsNull,
                                documentAccountLinesByAccountAndReferenceAndLetterIsNull, letter,
                                documentAccountLineDao, literableDocumentAccountLinePage);
                    }
                    List<DocumentAccountLine> lettrableLines = documentAccountLinesByAccountAndReferenceNotEmptyAndLetterIsNull
                            .stream().filter(line -> StringUtils.isNotBlank(line.getLetter()))
                            .collect(Collectors.toList());

                    List<Long> listLineIds = lettrableLines.stream().map(DocumentAccountLine::getId)
                            .collect(Collectors.toList());
                    if (listLineIds.isEmpty()) {
                        listLineIds.add(0L);
                    }

                    List<DocumentAccountLine> documentAccountLines = documentAccountLineDao
                            .findByAccountCodeAndLetterIsNullForLettring(listLineIds,
                                    accountCodePage.getContent().get(NumberConstant.ZERO), startDate, endDate,
                                    literableLinePage * literableLinePageSize, literableLinePageSize);

                    documentAccountLines.stream().forEach(line -> {
                        Optional<DocumentAccountLine> documentAccounLettrableLine = lettrableLines.stream()
                                .filter(lettrableLine -> lettrableLine.getId().equals(line.getId())).findFirst();
                        if (documentAccounLettrableLine.isPresent()) {
                            line.setLetter(documentAccounLettrableLine.get().getLetter());
                        }

                    });
                    Long documentAccountLinesCount = documentAccountLineDao.findByAccountCodeAndLetterIsNullForLettring(
                            accountCodePage.getContent().get(NumberConstant.ZERO), startDate, endDate);
                    LettringUtil.fillLiterableLinesUsingDocumentAccountLines(literableDocumentAccountLines,
                            documentAccountLines);
                    LettringUtil.sortLiterableDocumentAccountLinesByLetter(literableDocumentAccountLines);
                    literableDocumentAccountLinePage.setContent(literableDocumentAccountLines);
                    literableDocumentAccountLinePage.setTotalElementsOfAccounts(accountCodePage.getTotalElements());
                    literableDocumentAccountLinePage
                            .setTotalElementsOfDocumentAccountLinesPerAccount(documentAccountLinesCount);
                } else {
                    literableDocumentAccountLinePage.setContent(new ArrayList<>());
                }
                return literableDocumentAccountLinePage;
            } else {
                log.error(BEGIN_ACCOUNT_MUST_NOT_GRATER_THAN_END_ACCOUNT);
                throw new HttpCustomException(ApiErrors.Accounting.BEGIN_ACCOUNT_CODE_IS_GREATER_THAN_END_ACCOUNT);
            }
        } else {
            log.error(DATES_ORDER_INVALID);
            throw new HttpCustomException(ApiErrors.Accounting.START_DATE_IS_AFTER_END_DATE);
        }
    }

    @Override
    public List<LiterableDocumentAccountLineDto> saveLettersToSelectedLiterableDocumentAccountLine(
            List<LiterableDocumentAccountLineDto> selectedLiterableDocumentAccountLine) {
        Map<String, List<LiterableDocumentAccountLineDto>> linesGroupedByLetter = selectedLiterableDocumentAccountLine
                .stream().collect(Collectors.groupingBy(LiterableDocumentAccountLineDto::getLetter));
        for (Map.Entry<String, List<LiterableDocumentAccountLineDto>> entry : linesGroupedByLetter.entrySet()) {
            BigDecimal sumCredit = linesGroupedByLetter.get(entry.getKey()).stream()
                    .map(LiterableDocumentAccountLineDto::getCredit).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(AccountingConstants.DEFAULT_SCALE_FOR_BIG_DECIMAL, RoundingMode.HALF_UP);
            BigDecimal sumDebit = linesGroupedByLetter.get(entry.getKey()).stream()
                    .map(LiterableDocumentAccountLineDto::getDebit).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(AccountingConstants.DEFAULT_SCALE_FOR_BIG_DECIMAL, RoundingMode.HALF_UP);
            long numberOfAccountForLetter = linesGroupedByLetter.get(entry.getKey()).stream()
                    .map(LiterableDocumentAccountLineDto::getAccount).distinct().count();
            if (numberOfAccountForLetter > AccountingConstants.MAXIMUM_NUMBER_OF_ACCOUNT_PER_LETTER) {
                log.error(YOU_MUST_CHOOSE_FOR_THE_THE_SAME_LETTER_SINGLE_ACCOUNT,
                        linesGroupedByLetter.get(entry.getKey()).get(NumberConstant.ZERO));
                throw new HttpCustomException(
                        ApiErrors.Accounting.YOU_MUST_CHOOSE_FOR_THE_THE_SAME_LETTER_SINGLE_ACCOUNT,
                        new ErrorsResponse().error(linesGroupedByLetter.get(entry.getKey()).get(NumberConstant.ZERO)));
            } else if (!sumCredit.equals(sumDebit)) {
                log.error(TOTAL_DEBIT_SHOULD_BE_EQUAL_TO_TOTAL_CREDIT_FOR_ACCOUNT_AND_LETTER,
                        linesGroupedByLetter.get(entry.getKey()).get(NumberConstant.ZERO));
                throw new HttpCustomException(
                        ApiErrors.Accounting.TOTAL_DEBIT_SHOULD_BE_EQUAL_TO_TOTAL_CREDIT_FOR_ACCOUNT_AND_LETTER,
                        new ErrorsResponse().error(linesGroupedByLetter.get(entry.getKey()).get(NumberConstant.ZERO)));
            }
        }

        Long currentFiscalYear = accountingConfigurationService.getCurrentFiscalYearId();
        List<DocumentAccountLine> documentAccountLines = new ArrayList<>();
        selectedLiterableDocumentAccountLine.forEach((LiterableDocumentAccountLineDto line) -> {
            DocumentAccountingDto documentAccount = documentAccountService
                    .getDocumentAccount(line.getDocumentAccount());
            if (fiscalYearService.isDateInClosedPeriod(documentAccount.getDocumentDate(), currentFiscalYear)) {
                log.error("Trying to letter a line contained in a closed period");
                throw new HttpCustomException(ApiErrors.Accounting.LETTERING_OPERATION_IN_CLOSED_PERIOD,
                        new ErrorsResponse().error(line.getLetter()));
            }
            DocumentAccountLine documentAccountLine = documentAccountLineDao.findByIdAndIsDeletedFalse(line.getId());
            documentAccountLine.setLetter(line.getLetter());
            log.error("Lettering line with id {} using letter code {}", documentAccountLine.getId(),
                    documentAccountLine.getLetter());
            documentAccountLines.add(documentAccountLine);
        });
        documentAccountLineService.saveDocumentAccountLines(documentAccountLines);
        return selectedLiterableDocumentAccountLine;
    }

    @Override
    public String generateFirstUnusedLetter() {
        List<String> existingLetters = documentAccountLineDao.getAllLetter();
        String firstUnusedLetter = AccountingConstants.FIRST_LETTERING_CODE;
        for (String letter : existingLetters) {
            if (!letter.equalsIgnoreCase(firstUnusedLetter)) {
                return firstUnusedLetter;
            }
            firstUnusedLetter = LettringUtil.incrementLetter(firstUnusedLetter, documentAccountLineDao);
        }
        return firstUnusedLetter;
    }

    @Override
    public List<LiterableDocumentAccountLineDto> removeLettersFromDeselectedDocumentAccountLine(
            List<LiterableDocumentAccountLineDto> deselectedDocumentAccountLine) {
        Long currentFiscalYear = accountingConfigurationService.getCurrentFiscalYearId();
        List<DocumentAccountLine> documentAccountLines = new ArrayList<>();
        deselectedDocumentAccountLine.forEach((LiterableDocumentAccountLineDto letteringDto) -> {
            Optional<DocumentAccountLine> documentAccountLine = documentAccountLineDao.findById(letteringDto.getId());
            if (!documentAccountLine.isPresent()) {
                throw new HttpCustomException(ApiErrors.Accounting.DOCUMENT_ACCOUNT_LINE_NOT_FOUND,
                        new ErrorsResponse().error(letteringDto.getId()));
            }
            DocumentAccountingDto documentAccount = documentAccountService
                    .getDocumentAccount(letteringDto.getDocumentAccount());
            if (fiscalYearService.isDateInClosedPeriod(documentAccount.getDocumentDate(), currentFiscalYear)) {
                log.error("Trying to de-letter a line contained in a closed period");
                throw new HttpCustomException(ApiErrors.Accounting.LETTERING_OPERATION_IN_CLOSED_PERIOD,
                        new ErrorsResponse().error(documentAccountLine.get().getLetter()));
            }
            log.error("de-lettering line with id {} and previous letter code {}", documentAccountLine.get().getId(),
                    documentAccountLine.get().getLetter());
            documentAccountLine.get().setLetter(null);
            documentAccountLines.add(documentAccountLine.get());
        });
        documentAccountLineService.saveDocumentAccountLines(documentAccountLines);
        return deselectedDocumentAccountLine;
    }
}
