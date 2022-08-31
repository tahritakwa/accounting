package fr.sparkit.accounting.services.impl;

import static fr.sparkit.accounting.constants.AccountingConstants.DD_MM_YYYY;
import static fr.sparkit.accounting.constants.AccountingConstants.SUB_TOTAL_PARAM;
import static fr.sparkit.accounting.util.CalculationUtil.getFormattedBigDecimalValueOrEmptyStringIfZero;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import fr.sparkit.accounting.services.utils.TraductionServiceUtil;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.sparkit.accounting.constants.AccountingConstants;
import fr.sparkit.accounting.dao.DocumentAccountLineDao;
import fr.sparkit.accounting.dto.AccountBalanceDto;
import fr.sparkit.accounting.dto.GeneralLedgerAccountDetailsDto;
import fr.sparkit.accounting.dto.GeneralLedgerAccountDto;
import fr.sparkit.accounting.dto.GeneralLedgerAmountDto;
import fr.sparkit.accounting.dto.GeneralLedgerDetailsPageDto;
import fr.sparkit.accounting.dto.GeneralLedgerPageDto;
import fr.sparkit.accounting.dto.GeneralLedgerReportLineDto;
import fr.sparkit.accounting.entities.Account;
import fr.sparkit.accounting.entities.DocumentAccount;
import fr.sparkit.accounting.entities.DocumentAccountLine;
import fr.sparkit.accounting.enumuration.AccountTypes;
import fr.sparkit.accounting.services.IAccountingConfigurationService;
import fr.sparkit.accounting.services.IDocumentAccountLineService;
import fr.sparkit.accounting.services.IGeneralLedgerService;
import fr.sparkit.accounting.services.utils.AccountingServiceUtil;
import fr.sparkit.accounting.util.CalculationUtil;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class GeneralLedgerService implements IGeneralLedgerService {

    private final DocumentAccountLineDao documentAccountLineDao;

    @Autowired
    public GeneralLedgerService(DocumentAccountLineDao documentAccountLineDao,
            IAccountingConfigurationService accountingConfigurationService,
            IDocumentAccountLineService documentAccountLineService) {
        super();
        this.documentAccountLineDao = documentAccountLineDao;
    }

    @Cacheable(value = "GeneralLedgerAccounts", key = "'GeneralLedgerAccounts_'"
            + "+T(fr.sparkit.accounting.util.CompanyContextHolder).getCompanyContext()+'_'+T(java.util.Arrays).toString(#root.args)")
    @Override
    public GeneralLedgerPageDto findGeneralLedgerAccounts(int page, int size, LocalDateTime startDate,
            LocalDateTime endDate, String beginAccountCode, String endAccountCode, String beginAmount, String endAmount,
            String accountType, String letteringOperationType) {

        AccountingServiceUtil.checkFilterOnDates(startDate, endDate);

        AccountingServiceUtil.checkFilterOnAccounts(beginAccountCode, endAccountCode);

        AccountingServiceUtil.checkFilterOnAmounts(beginAmount, endAmount);

        int parseBeginAccountCode = AccountingServiceUtil.getDefaultAccountCode(beginAccountCode,
                AccountingConstants.MIN_ACCOUNT_CODE);
        int parseEndAccountCode = AccountingServiceUtil.getDefaultAccountCode(endAccountCode,
                AccountingConstants.GENERAL_LEDGER_MAX_ACCOUNT_CODE);

        Page<Long> accountCodesPage = null;
        Pageable pageable = PageRequest.of(page, size);

        BigDecimal bigDecimalParseBeginAmount = AccountingServiceUtil.getBigDecimalAmount(beginAmount, BigDecimal.ZERO);

        if (!endAmount.isEmpty()) {
            BigDecimal bigDecimalParseEndAmount = new BigDecimal(endAmount);
            accountCodesPage = documentAccountLineDao.getAllAccountsInCurrentDatesAndAccountCodesAndAmounts(startDate,
                    endDate, parseBeginAccountCode, parseEndAccountCode, bigDecimalParseBeginAmount,
                    bigDecimalParseEndAmount, AccountTypes.geStateOfAccountByType(accountType), pageable);
        } else {
            accountCodesPage = documentAccountLineDao.getAllAccountsInCurrentDatesAndAccountCodesWithoutEndAmount(
                    startDate, endDate, parseBeginAccountCode, parseEndAccountCode, bigDecimalParseBeginAmount,
                    AccountTypes.geStateOfAccountByType(accountType), pageable);
        }
        List<Long> accountCodes = accountCodesPage.getContent();
        Long accountsTotalElements = accountCodesPage.getTotalElements();

        List<GeneralLedgerAccountDto> generalLedgerAccountDtos = new ArrayList<>();

        BigDecimal totalDebitGeneralLedger = BigDecimal.ZERO;
        BigDecimal totalCreditGeneralLedger = BigDecimal.ZERO;

        if (!accountCodes.isEmpty()) {
            List<AccountBalanceDto> accountBalanceDtos;
            GeneralLedgerAmountDto generalLedgerAmountDto;
            if (!endAmount.isEmpty()) {
                BigDecimal bigDecimalParseEndAmount = new BigDecimal(endAmount);
                accountBalanceDtos = documentAccountLineDao.getGeneralLedgerAccountInDatesAndAmounts(accountCodes,
                        startDate, endDate, bigDecimalParseBeginAmount, bigDecimalParseEndAmount);

                generalLedgerAmountDto =  getGeneralLedgerTotalAmountsInDatesAndAccountCodesAndAmountsLettring(startDate, endDate,
                                parseBeginAccountCode, parseEndAccountCode, bigDecimalParseBeginAmount,
                        bigDecimalParseEndAmount, AccountTypes.geStateOfAccountByType(accountType),
                        letteringOperationType);

            } else {
                accountBalanceDtos = documentAccountLineDao.getGeneralLedgerAccountInDatesWithoutEndAmount(accountCodes,
                        startDate, endDate, bigDecimalParseBeginAmount);

                generalLedgerAmountDto = getGeneralLedgerTotalAmountsInDatesAndAccountCodesWithoutEndAmountLettring(
                        startDate, endDate,
                                parseBeginAccountCode, parseEndAccountCode, bigDecimalParseBeginAmount,
                        AccountTypes.geStateOfAccountByType(accountType), letteringOperationType);

            }
            totalDebitGeneralLedger = generalLedgerAmountDto.getTotalDebit() != null
                    ? generalLedgerAmountDto.getTotalDebit()
                    : BigDecimal.ZERO;
            totalCreditGeneralLedger = generalLedgerAmountDto.getTotalCredit() != null
                    ? generalLedgerAmountDto.getTotalCredit()
                    : BigDecimal.ZERO;

            accountBalanceDtos.forEach((AccountBalanceDto accountBalanceDto) -> {
                BigDecimal totalDebit = accountBalanceDto.getTotalCurrentDebit();
                BigDecimal totalCredit = accountBalanceDto.getTotalCurrentCredit();
                BigDecimal totalBalance = totalDebit.subtract(totalCredit);
                generalLedgerAccountDtos.add(new GeneralLedgerAccountDto(accountBalanceDto.getAccountId(),
                        accountBalanceDto.getAccountCode(), accountBalanceDto.getAccountLabel(), totalDebit,
                        totalCredit, totalBalance, accountBalanceDto.isLiterable()));
            });
        }

        generalLedgerAccountDtos.add(new GeneralLedgerAccountDto(totalDebitGeneralLedger, totalCreditGeneralLedger,
                totalDebitGeneralLedger.subtract(totalCreditGeneralLedger)));

        return new GeneralLedgerPageDto(generalLedgerAccountDtos, accountsTotalElements);
    }

    private GeneralLedgerAmountDto getGeneralLedgerTotalAmountsInDatesAndAccountCodesAndAmountsLettring(
            LocalDateTime startDate, LocalDateTime endDate, int beginAccountCode, int endAccountCode,
            BigDecimal beginAmount, BigDecimal endAmount, Boolean accountType, String letteringOperationType) {
        if (AccountingConstants.LETTER.equals(letteringOperationType)) {
            return documentAccountLineDao.getGeneralLedgerTotalAmountsInDatesAndAccountCodesAndAmountsLetteredLines(
                    startDate, endDate, beginAccountCode, endAccountCode, beginAmount, endAmount, accountType);
        } else if (AccountingConstants.NOT_LITERATE.equals(letteringOperationType)) {
            return documentAccountLineDao.getGeneralLedgerTotalAmountsInDatesAndAccountCodesAndAmountsNotLetteredLines(
                    startDate, endDate, beginAccountCode, endAccountCode, beginAmount, endAmount, accountType);
        }
        return documentAccountLineDao.getGeneralLedgerTotalAmountsInDatesAndAccountCodesAndAmounts(startDate, endDate,
                beginAccountCode, endAccountCode, beginAmount, endAmount, accountType);
    }

    private GeneralLedgerAmountDto getGeneralLedgerTotalAmountsInDatesAndAccountCodesWithoutEndAmountLettring(
            LocalDateTime startDate, LocalDateTime endDate, int beginAccountCode, int endAccountCode,
            BigDecimal beginAmount, Boolean accountType, String letteringOperationType) {
        if (AccountingConstants.LETTER.equals(letteringOperationType)) {
            return documentAccountLineDao
                    .getGeneralLedgerTotalAmountsInDatesAndAccountCodesWithoutEndAmountLetteredLines(startDate,
                    endDate, beginAccountCode, endAccountCode, beginAmount, accountType);
        } else if (AccountingConstants.NOT_LITERATE.equals(letteringOperationType)) {
            return documentAccountLineDao
                    .getGeneralLedgerTotalAmountsInDatesAndAccountCodesWithoutEndAmountNotLetteredLines(startDate,
                    endDate, beginAccountCode, endAccountCode, beginAmount, accountType);
        }
        return documentAccountLineDao
        .getGeneralLedgerTotalAmountsInDatesAndAccountCodesWithoutEndAmount(startDate, endDate,
                        beginAccountCode, endAccountCode, beginAmount, accountType);
    }

    @Cacheable(value = "GeneralLedgerAccountDetails", key = "'GeneralLedgerAccountDetails_'"
            + "+T(fr.sparkit.accounting.util.CompanyContextHolder).getCompanyContext()+'_'+T(java.util.Arrays).toString(#root.args)")
    @Override
    public GeneralLedgerDetailsPageDto findGeneralLedgerAccountDetails(Long accountId, int page, int size,
            LocalDateTime startDate, LocalDateTime endDate, String beginAmount, String endAmount,
            String letteringOperationType, String field, String direction) {
        Pageable pageable;
        Sort sortField;
        if (StringUtils.isNoneBlank(field) && StringUtils.isNoneBlank(direction)) {
            sortField = getEntityFieldNameSort(field,
                    Sort.Direction.valueOf(direction.toUpperCase(AccountingConstants.LANGUAGE)));
            pageable = PageRequest.of(page, size, sortField);
        } else {
            String[] orders = { "(CONVERT (varchar(8), da.documentAccount.documentDate, 112))",
                    "documentAccount.codeDocument", "id" };
            sortField = JpaSort.unsafe(Sort.Direction.ASC, orders);
            pageable = PageRequest.of(page, size, sortField);
        }

        Page<DocumentAccountLine> documentAccountLines;

        BigDecimal bigDecimalParseBeginAmount = AccountingServiceUtil.getBigDecimalAmount(beginAmount, BigDecimal.ZERO);

        List<GeneralLedgerAccountDetailsDto> generalLedgerAccountDetails = new ArrayList<>();
        BigDecimal lastBalance;

        if (!endAmount.isEmpty()) {
            BigDecimal bigDecimalParseEndAmount = new BigDecimal(endAmount);
            documentAccountLines = findByAccountIdInCurrentDatesAndAccountCodesAndAmountsLettring(accountId,
                    bigDecimalParseBeginAmount, bigDecimalParseEndAmount, startDate, endDate, pageable,
                    letteringOperationType);
            BigDecimal balance = BigDecimal.ZERO;
            if (size * page > 0) {
            Pageable balencePageable = PageRequest.of(0, size * page, sortField);
            Page<DocumentAccountLine> documentLineList = findByAccountIdInCurrentDatesAndAccountCodesAndAmountsLettring(
                    accountId, bigDecimalParseBeginAmount, bigDecimalParseEndAmount, startDate, endDate,
                    balencePageable,
                    letteringOperationType);

            documentLineList.stream()
                    .forEach(line -> balance.add(line.getDebitAmount()).subtract(line.getCreditAmount()));
            }
            lastBalance = balance;

        } else {
            documentAccountLines = findByAccountIdInCurrentDatesAndAccountCodesWithoutEndAmountLettring(accountId,
                    bigDecimalParseBeginAmount, startDate, endDate, pageable, letteringOperationType);
            AtomicReference<BigDecimal> balance = new AtomicReference<>(BigDecimal.ZERO);
            if (size * page > 0) {
            Pageable balencePageable = PageRequest.of(0, size, sortField);
            Page<DocumentAccountLine> documentLineList = findByAccountIdInCurrentDatesAndAccountCodesWithoutEndAmountLettring(
                    accountId, bigDecimalParseBeginAmount, startDate, endDate, balencePageable, letteringOperationType);
            documentLineList.stream()
                    .forEach(line ->  balance.set(balance.get().add(line.getDebitAmount()).subtract(line.getCreditAmount())));
            }
            lastBalance = balance.get();

        }

        Long documentAccountLinesTotalElements = documentAccountLines.getTotalElements();

        if (!documentAccountLines.isEmpty()) {
            for (DocumentAccountLine documentAccountLine : documentAccountLines) {
                addToGeneralLedgerAccountDetails(documentAccountLine, generalLedgerAccountDetails, lastBalance);
                lastBalance = generalLedgerAccountDetails.get(generalLedgerAccountDetails.size() - 1).getBalance();
            }
        }
        return new GeneralLedgerDetailsPageDto(generalLedgerAccountDetails, documentAccountLinesTotalElements);
    }

    private Sort getEntityFieldNameSort(String field, Direction direction) {
        switch (field) {
        case "documentAccountDate":
            return Sort.by(new Sort.Order(direction, "documentAccount.documentDate").ignoreCase());
        case "documentAccountCode":
            return Sort.by(new Sort.Order(direction, "documentAccount.codeDocument").ignoreCase());
        case "documentAccountJournal":
            return Sort.by(new Sort.Order(direction, "documentAccount.journal.label").ignoreCase());
        case "debit":
            return Sort.by(new Sort.Order(direction, "debitAmount"));
        case "credit":
            return Sort.by(new Sort.Order(direction, "creditAmount"));
        case "balance":
            return Sort.by(new Sort.Order(direction, "debitAmount"));
        default:
            break;
        }
        return Sort.by(new Sort.Order(direction, field).ignoreCase());
    }

    private Page<DocumentAccountLine> findByAccountIdInCurrentDatesAndAccountCodesAndAmountsLettring(Long accountId,
            BigDecimal beginAmount, BigDecimal endAmount, LocalDateTime startDate, LocalDateTime endDate,
            Pageable pageable, String letteringOperationType) {
        if (AccountingConstants.LETTER.equals(letteringOperationType)) {
            return documentAccountLineDao.findByAccountIdInCurrentDatesAndAccountCodesAndAmountsLettring(accountId,
                    beginAmount, endAmount, startDate, endDate, pageable);
        } else if (AccountingConstants.NOT_LITERATE.equals(letteringOperationType)) {
            return documentAccountLineDao.findByAccountIdInCurrentDatesAndAccountCodesAndAmountsNotLettring(accountId,
                    beginAmount, endAmount, startDate, endDate, pageable);
        }
        return documentAccountLineDao.findByAccountIdInCurrentDatesAndAccountCodesAndAmounts(accountId, beginAmount,
                endAmount, startDate, endDate, pageable);
    }

    private Page<DocumentAccountLine> findByAccountIdInCurrentDatesAndAccountCodesWithoutEndAmountLettring(
            Long accountId, BigDecimal beginAmount, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable,
            String letteringOperationType) {
        if (AccountingConstants.LETTER.equals(letteringOperationType)) {
            return documentAccountLineDao.findByAccountIdInCurrentDatesAndAccountCodesWithoutEndAmountLettring(
                    accountId, beginAmount, startDate, endDate, pageable);
        } else if (AccountingConstants.NOT_LITERATE.equals(letteringOperationType)) {
            return documentAccountLineDao.findByAccountIdInCurrentDatesAndAccountCodesWithoutEndAmountNotLettring(
                    accountId, beginAmount, startDate, endDate, pageable);
        }
        return documentAccountLineDao.findByAccountIdInCurrentDatesAndAccountCodesWithoutEndAmount(accountId,
                beginAmount, startDate, endDate, pageable);
    }
    @Override
    public void addToGeneralLedgerAccountDetails(DocumentAccountLine documentAccountLine,
            List<GeneralLedgerAccountDetailsDto> generalLedgerDetails, BigDecimal lastBalance) {
        BigDecimal debit = documentAccountLine.getDebitAmount();
        BigDecimal credit = documentAccountLine.getCreditAmount();
        DocumentAccount documentAccount = documentAccountLine.getDocumentAccount();
        generalLedgerDetails.add(new GeneralLedgerAccountDetailsDto(documentAccountLine.getId(),
                documentAccount.getId(), documentAccount.getCodeDocument(), documentAccount.getDocumentDate(),
                documentAccountLine.getLabel(), documentAccount.getJournal().getLabel(), debit, credit,
                lastBalance.add(debit.subtract(credit))));
    }

    @Override
    public List<GeneralLedgerReportLineDto> generateGeneralLedgerTelerikReport(LocalDateTime startDate,
            LocalDateTime endDate, String beginAccountCode, String endAccountCode, String beginAmount, String endAmount,
            String accountType, String letteringOperationType, String field, String direction) {

        int parseBeginAccountCode = AccountingServiceUtil.getDefaultAccountCode(beginAccountCode,
                AccountingConstants.MIN_ACCOUNT_CODE);
        int parseEndAccountCode = AccountingServiceUtil.getDefaultAccountCode(endAccountCode,
                AccountingConstants.GENERAL_LEDGER_MAX_ACCOUNT_CODE);

        BigDecimal bigDecimalParseBeginAmount = AccountingServiceUtil.getBigDecimalAmount(beginAmount, BigDecimal.ZERO);

        List<DocumentAccountLine> documentAccountLines;
        Sort sortField;
        if (StringUtils.isNoneBlank(field) && StringUtils.isNoneBlank(direction)) {
            sortField = getEntityFieldNameSort(field,
                    Sort.Direction.valueOf(direction.toUpperCase(AccountingConstants.LANGUAGE)));
        } else {
            String[] orders = { "(CONVERT (varchar(8), documentAccount.documentDate, 112))",
                    "documentAccount.codeDocument", "id" };
            sortField = JpaSort.unsafe(Sort.Direction.ASC, orders);
        }
        if (!endAmount.isEmpty()) {
            BigDecimal bigDecimalParseEndAmount = new BigDecimal(endAmount);
            documentAccountLines = findAllByAccountIdInCurrentDatesAndAccountCodesAndAmountsLettring(
                    startDate, endDate, parseBeginAccountCode, parseEndAccountCode, bigDecimalParseBeginAmount,
                    bigDecimalParseEndAmount, AccountTypes.geStateOfAccountByType(accountType), letteringOperationType,
                    sortField);
        } else {
            documentAccountLines = findAllByAccountIdInCurrentDatesAndAccountCodesWithoutEndAmountLettring(startDate,
                    endDate,
                            parseBeginAccountCode, parseEndAccountCode, bigDecimalParseBeginAmount,
                    AccountTypes.geStateOfAccountByType(accountType), letteringOperationType, sortField);
        }

        List<Account> accounts = documentAccountLines.stream().map(DocumentAccountLine::getAccount).distinct()
                .sorted(Comparator.comparingInt(Account::getCode)).collect(Collectors.toList());

        List<GeneralLedgerReportLineDto> generalLedgerReportLines = new ArrayList<>();

        BigDecimal totalCreditGeneralLedger = BigDecimal.ZERO;
        BigDecimal totalDebitGeneralLedger = BigDecimal.ZERO;
        for (Account account : accounts) {

            List<DocumentAccountLine> documentAccountLinesByAccountId = documentAccountLines.stream()
                    .filter((DocumentAccountLine documentAccountLine) -> documentAccountLine.getAccount().getId()
                            .equals(account.getId()))
                    .collect(Collectors.toList());

            BigDecimal totalDebitAccount = documentAccountLinesByAccountId.stream()
                    .map(DocumentAccountLine::getDebitAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalCreditAccount = documentAccountLinesByAccountId.stream()
                    .map(DocumentAccountLine::getCreditAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalBalanceAccount = totalDebitAccount.subtract(totalCreditAccount);

            if (isTotalDebitOrTotalCreditGreaterThanZero(totalDebitAccount, totalCreditAccount)) {
                GeneralLedgerAccountDto generalLedgerAccountDto = new GeneralLedgerAccountDto(account.getId(),
                        account.getCode(), account.getLabel(), totalDebitAccount, totalCreditAccount,
                        totalBalanceAccount, account.isLiterable());
                insertAccountCodeAndLabelLineIntoReport(generalLedgerReportLines, generalLedgerAccountDto);

                insertGeneralLedgerLinesDetailsIntoReport(documentAccountLines, generalLedgerReportLines, account);

                insertSubTotalAccountLineIntoReport(generalLedgerReportLines, generalLedgerAccountDto);

                totalCreditGeneralLedger = totalCreditGeneralLedger.add(generalLedgerAccountDto.getTotalCredit());
                totalDebitGeneralLedger = totalDebitGeneralLedger.add(generalLedgerAccountDto.getTotalDebit());
            }
        }

        insertGeneralLedgerTotalLineIntoReport(generalLedgerReportLines, endDate, totalCreditGeneralLedger,
                totalDebitGeneralLedger);

        return generalLedgerReportLines;
    }

    private List<DocumentAccountLine> findAllByAccountIdInCurrentDatesAndAccountCodesAndAmountsLettring(
            LocalDateTime startDate, LocalDateTime endDate, int beginAccountCode, int endAccountCode,
            BigDecimal beginAmount, BigDecimal endAmount, Boolean accountType, String letteringOperationType,
            Sort sortField) {
        if (AccountingConstants.LETTER.equals(letteringOperationType)) {
            return documentAccountLineDao.findAllDocumentAccountLinesInDatesAndAccountCodesAndAmountsLettering(
                    startDate,
                    endDate, beginAccountCode, endAccountCode, beginAmount, endAmount, accountType, sortField);
        } else if (AccountingConstants.NOT_LITERATE.equals(letteringOperationType)) {
            return documentAccountLineDao.findAllDocumentAccountLinesInDatesAndAccountCodesAndAmountsNotLettering(
                    startDate,
                    endDate, beginAccountCode, endAccountCode, beginAmount, endAmount, accountType, sortField);
        }
        return documentAccountLineDao.findAllDocumentAccountLinesInDatesAndAccountCodesAndAmounts(startDate, endDate,
                beginAccountCode, endAccountCode, beginAmount, endAmount, accountType, sortField);
    }

    private List<DocumentAccountLine> findAllByAccountIdInCurrentDatesAndAccountCodesWithoutEndAmountLettring(
            LocalDateTime startDate, LocalDateTime endDate, int beginAccountCode, int endAccountCode,
            BigDecimal beginAmount, Boolean accountType, String letteringOperationType, Sort sortField) {

        if (AccountingConstants.LETTER.equals(letteringOperationType)) {
            return documentAccountLineDao.findAllDocumentAccountLinesInDatesAndAccountCodesWithoutEndAmountLettering(
                    startDate, endDate, beginAccountCode, endAccountCode, beginAmount, accountType, sortField);
        } else if (AccountingConstants.NOT_LITERATE.equals(letteringOperationType)) {
            return documentAccountLineDao.findAllDocumentAccountLinesInDatesAndAccountCodesWithoutEndAmountNotLettering(
                    startDate, endDate, beginAccountCode, endAccountCode, beginAmount, accountType, sortField);
        }
        return documentAccountLineDao.findAllDocumentAccountLinesInDatesAndAccountCodesWithoutEndAmount(startDate,
                endDate, beginAccountCode, endAccountCode, beginAmount, accountType, sortField);
    }

    public boolean isTotalDebitOrTotalCreditGreaterThanZero(BigDecimal totalDebitAccount,
            BigDecimal totalCreditAccount) {
        return !BigDecimal.ZERO.equals(totalDebitAccount) || !BigDecimal.ZERO.equals(totalCreditAccount);
    }

    private void insertGeneralLedgerLinesDetailsIntoReport(List<DocumentAccountLine> documentAccountLines,
            List<GeneralLedgerReportLineDto> generalLedgerReport, Account account) {
        List<DocumentAccountLine> accountDocumentAccountLines = documentAccountLines.stream()
                .filter(documentAccountLine -> documentAccountLine.getAccount().getId().equals(account.getId()))
                .collect(Collectors.toList());

        BigDecimal accountLineBalance = BigDecimal.ZERO;

        if (accountDocumentAccountLines != null && !accountDocumentAccountLines.isEmpty()) {
            for (DocumentAccountLine documentAccountLine : accountDocumentAccountLines) {
                accountLineBalance = accountLineBalance
                        .add(documentAccountLine.getDebitAmount().subtract(documentAccountLine.getCreditAmount()));
                insertDocumentAccountLineIntoReport(accountLineBalance, generalLedgerReport, documentAccountLine);
            }
        }
    }

    @Override
    public void insertDocumentAccountLineIntoReport(BigDecimal balance,
            List<GeneralLedgerReportLineDto> generalLedgerReport, DocumentAccountLine documentAccountLine) {
        DocumentAccount documentAccount = documentAccountLine.getDocumentAccount();
        BigDecimal debit = documentAccountLine.getDebitAmount();
        BigDecimal credit = documentAccountLine.getCreditAmount();
        generalLedgerReport.add(new GeneralLedgerReportLineDto(
                documentAccount.getDocumentDate().format(DateTimeFormatter.ofPattern(DD_MM_YYYY)),
                documentAccountLine.getLabel(), documentAccount.getCodeDocument(),
                documentAccount.getJournal().getLabel(), getFormattedBigDecimalValueOrEmptyStringIfZero(debit),
                getFormattedBigDecimalValueOrEmptyStringIfZero(credit),
                CalculationUtil.getFormattedBigDecimalValueOrEmptyString(balance)));
    }

    @Override
    public void insertGeneralLedgerTotalLineIntoReport(List<GeneralLedgerReportLineDto> generalLedgerReport,
            LocalDateTime endDate, BigDecimal totalCredit, BigDecimal totalDebit) {
        BigDecimal totalBalance = totalDebit.subtract(totalCredit);
        generalLedgerReport.add(new GeneralLedgerReportLineDto(AccountingConstants.GENERAL_LEDGER_TOTAL,
                getFormattedBigDecimalValueOrEmptyStringIfZero(totalDebit),
                getFormattedBigDecimalValueOrEmptyStringIfZero(totalCredit),
                CalculationUtil.getAccountingDecimalFormat().format(totalBalance)));
    }

    @Override
    public void insertSubTotalAccountLineIntoReport(List<GeneralLedgerReportLineDto> generalLedgerReport,
            GeneralLedgerAccountDto generalLedgerAccountDto) {
        generalLedgerReport.add(new GeneralLedgerReportLineDto(
                TraductionServiceUtil.getI18nResourceBundle().getString(SUB_TOTAL_PARAM),
                getFormattedBigDecimalValueOrEmptyStringIfZero(generalLedgerAccountDto.getTotalDebit()),
                getFormattedBigDecimalValueOrEmptyStringIfZero(generalLedgerAccountDto.getTotalCredit()),
                CalculationUtil.getAccountingDecimalFormat().format(generalLedgerAccountDto.getTotalBalance())));
    }

    @Override
    public BigDecimal calculateTotalCreditGeneralLedger(List<GeneralLedgerAccountDto> generalLedgerAccounts) {
        return generalLedgerAccounts.stream().map(GeneralLedgerAccountDto::getTotalCredit).reduce(BigDecimal.ZERO,
                BigDecimal::add);
    }

    @Override
    public BigDecimal calculateTotalDebitGeneralLedger(List<GeneralLedgerAccountDto> generalLedgerAccounts) {
        return generalLedgerAccounts.stream().map(GeneralLedgerAccountDto::getTotalDebit).reduce(BigDecimal.ZERO,
                BigDecimal::add);
    }

    public void insertAccountCodeAndLabelLineIntoReport(Collection<GeneralLedgerReportLineDto> generalLedgerReport,
            GeneralLedgerAccountDto generalLedgerAccountDto) {
        generalLedgerReport.add(new GeneralLedgerReportLineDto(AccountingServiceUtil.getConcatinationCodeWithLabel(
                String.valueOf(generalLedgerAccountDto.getAccountCode()), generalLedgerAccountDto.getAccountName())));
    }
}
