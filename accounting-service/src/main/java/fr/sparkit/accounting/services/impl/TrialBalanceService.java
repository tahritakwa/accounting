package fr.sparkit.accounting.services.impl;

import static fr.sparkit.accounting.constants.AccountingConstants.*;
import static fr.sparkit.accounting.constants.NumberConstant.*;
import static fr.sparkit.accounting.services.utils.AccountingServiceUtil.*;
import static fr.sparkit.accounting.services.utils.TrialBalanceUtil.*;
import static fr.sparkit.accounting.util.CalculationUtil.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.sparkit.accounting.convertor.AccountConvertor;
import fr.sparkit.accounting.dao.AccountDao;
import fr.sparkit.accounting.dao.DocumentAccountLineDao;
import fr.sparkit.accounting.dto.AccountBalanceDto;
import fr.sparkit.accounting.dto.AccountDto;
import fr.sparkit.accounting.dto.InitialTrialBalanceDto;
import fr.sparkit.accounting.dto.TrialBalanceAccountAmountDto;
import fr.sparkit.accounting.dto.TrialBalanceAccountDto;
import fr.sparkit.accounting.dto.TrialBalancePageDto;
import fr.sparkit.accounting.dto.TrialBalanceReportLineDto;
import fr.sparkit.accounting.entities.Account;
import fr.sparkit.accounting.entities.DocumentAccountLine;
import fr.sparkit.accounting.services.ITrialBalanceService;
import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.http.HttpCustomException;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class TrialBalanceService implements ITrialBalanceService {

    private final DocumentAccountLineDao documentAccountLineDao;
    private final AccountDao accountDao;

    @Autowired
    public TrialBalanceService(DocumentAccountLineDao documentAccountLineDao, AccountDao accountDao) {
        super();
        this.documentAccountLineDao = documentAccountLineDao;
        this.accountDao = accountDao;
    }

    @Cacheable(value = "TrialBalanceAccounts", key = "'TrialBalanceAccounts_'"
            + "+T(fr.sparkit.accounting.util.CompanyContextHolder).getCompanyContext()+'_'+T(java.util.Arrays).toString(#root.args)")
    @Override
    public TrialBalancePageDto findTrialBalanceAccounts(int page, int size, LocalDateTime startDate,
            LocalDateTime endDate, String beginAccountCode, String endAccountCode) {

        log.info(FILTER_PARAMETERS_ENTERED, page, size, startDate, endDate, beginAccountCode, endAccountCode);

        if (!isDateBeforeOrEquals(startDate, endDate)) {
            log.error(DATES_ORDER_INVALID);
            throw new HttpCustomException(ApiErrors.Accounting.START_DATE_IS_AFTER_END_DATE);
        }

        if (!isCodeLowerThanOrEquals(beginAccountCode, endAccountCode)) {
            log.error(BEGIN_ACCOUNT_MUST_NOT_GRATER_THAN_END_ACCOUNT);
            throw new HttpCustomException(ApiErrors.Accounting.BEGIN_ACCOUNT_CODE_IS_GREATER_THAN_END_ACCOUNT);
        }

        int parseBeginAccountCode = getDefaultAccountCode(beginAccountCode, MIN_ACCOUNT_CODE);
        int parseEndAccountCode = getDefaultAccountCode(endAccountCode, TRIAL_BALANCE_MAX_ACCOUNT_CODE);

        Pageable pageable = PageRequest.of(page, size);

        TrialBalancePageDto trialBalanceAccountsPage = getTrialBalanceAccounts(pageable, startDate, endDate,
                parseBeginAccountCode, parseEndAccountCode);

        List<TrialBalanceAccountDto> trialBalanceAccounts = trialBalanceAccountsPage.getContent();
        List<TrialBalanceAccountDto> trialBalanceAccountsContainingSubTotalClass = new ArrayList<>();
        long totalElements = trialBalanceAccountsPage.getTotalElements();

        if (trialBalanceAccounts != null && !trialBalanceAccounts.isEmpty()) {
            int currentClassNumber = getClassNumber(trialBalanceAccounts.get(ZERO));
            for (int i = 0; i < trialBalanceAccounts.size(); i++) {
                if (isTrialBalanceAccountNotInCurrentClassNumber(currentClassNumber, trialBalanceAccounts.get(i))) {
                    addSubTotalTrialBalanceToList(parseBeginAccountCode, parseEndAccountCode, startDate, endDate,
                            trialBalanceAccountsContainingSubTotalClass, currentClassNumber);
                    currentClassNumber = getClassNumber(trialBalanceAccounts.get(i));
                }
                trialBalanceAccountsContainingSubTotalClass.add(trialBalanceAccounts.get(i));
                if (isLastPage(page, size, totalElements) && i == trialBalanceAccounts.size() - ONE) {
                    addSubTotalTrialBalanceToList(parseBeginAccountCode, parseEndAccountCode, startDate, endDate,
                            trialBalanceAccountsContainingSubTotalClass, currentClassNumber);
                }
            }
        }

        addTotalTrialBalanceToList(parseBeginAccountCode, parseEndAccountCode, startDate, endDate,
                trialBalanceAccountsContainingSubTotalClass);

        return new TrialBalancePageDto(trialBalanceAccountsContainingSubTotalClass, totalElements);

    }

    private void addSubTotalTrialBalanceToList(int parseBeginAccountCode, int parseEndAccountCode,
            LocalDateTime startDate, LocalDateTime endDate, List<TrialBalanceAccountDto> trialBalanceAccounts,
            int classNumber) {

        List<TrialBalanceAccountAmountDto> trialBalanceSubTotalAmountsDtos = documentAccountLineDao
                .getTrialBalanceAccountAmountsInClassAndDatesAndAccountCodes(startDate, endDate, parseBeginAccountCode,
                        parseEndAccountCode, String.valueOf(classNumber));
        addTotalAmountsToTrialBalanceAccounts(trialBalanceAccounts, new AccountDto(classNumber, TOTAL_CLASS),
                trialBalanceSubTotalAmountsDtos);

    }

    private void addTotalTrialBalanceToList(int parseBeginAccountCode, int parseEndAccountCode, LocalDateTime startDate,
            LocalDateTime endDate, List<TrialBalanceAccountDto> trialBalanceAccounts) {

        List<TrialBalanceAccountAmountDto> trialBalanceTotalAmountsDtos = documentAccountLineDao
                .getTrialBalanceAccountAmountsInDatesAndAccountCodes(startDate, endDate, parseBeginAccountCode,
                        parseEndAccountCode);
        addTotalAmountsToTrialBalanceAccounts(trialBalanceAccounts, new AccountDto(), trialBalanceTotalAmountsDtos);

    }

    @Override
    public TrialBalancePageDto getTrialBalanceAccounts(Pageable pageable, LocalDateTime startDate,
            LocalDateTime endDate, int parseBeginAccountCode, int parseEndAccountCode) {

        Page<Long> accountCodesPage = documentAccountLineDao.getAllAccountsInCurrentDatesAndAccountCodes(
                parseBeginAccountCode, parseEndAccountCode, startDate, endDate, pageable);

        List<TrialBalanceAccountDto> trialBalanceAccounts = new ArrayList<>();

        List<Long> accountCodes = accountCodesPage.getContent();

        if (!accountCodes.isEmpty()) {
            List<AccountDto> accountDtos = accountDao.findAllByCodes(accountCodes);

            List<InitialTrialBalanceDto> initialTrialBalanceDtos = documentAccountLineDao
                    .initialTrialBalance(accountCodes, startDate, endDate);

            List<AccountBalanceDto> currentTrialBalanceDtos = documentAccountLineDao.currentTrialBalance(accountCodes,
                    startDate, endDate);

            addAllToTrialBalanceAccounts(accountDtos, initialTrialBalanceDtos, currentTrialBalanceDtos,
                    trialBalanceAccounts);
        }

        return new TrialBalancePageDto(trialBalanceAccounts, accountCodesPage.getTotalElements());
    }

    @Override
    public List<TrialBalanceReportLineDto> generateTrialBalanceTelerikReport(LocalDateTime startDate,
            LocalDateTime endDate, String beginAccountCode, String endAccountCode) {

        List<Account> accountsInDates = findAllDocumentAccountLineByCodesAndDates(startDate, endDate, beginAccountCode,
                endAccountCode).stream().map(DocumentAccountLine::getAccount).distinct().collect(Collectors.toList());

        List<TrialBalanceAccountDto> trialBalanceAccounts = new ArrayList<>();

        accountsInDates.stream().forEach((Account account) -> {
            List<TrialBalanceAccountAmountDto> trialBalanceAccountAmountsDtos = documentAccountLineDao
                    .getTrialBalanceAccountAmountsInDates(account.getId(), startDate, endDate);
            addTotalAmountsToTrialBalanceAccounts(trialBalanceAccounts, AccountConvertor.modelToDto(account),
                    trialBalanceAccountAmountsDtos);
        });

        List<TrialBalanceReportLineDto> trialBalanceReport = new ArrayList<>();

        if (!trialBalanceAccounts.isEmpty()) {
            TrialBalanceAccountDto trialBalanceResultDto = new TrialBalanceAccountDto();
            TrialBalanceAccountDto subTotalClassDto = new TrialBalanceAccountDto();
            int classNumber = getClassNumber(trialBalanceAccounts.get(ZERO));
            for (TrialBalanceAccountDto trialBalanceAccountDto : trialBalanceAccounts) {
                if (isTrialBalanceAccountNotInCurrentClassNumber(classNumber, trialBalanceAccountDto)) {
                    insertTrialBalanceSubTotalLineIntoReport(classNumber, subTotalClassDto, trialBalanceReport);
                    setAccumulationInTrialBalanceResultIfClassIsProductOrExpense(trialBalanceResultDto, classNumber,
                            subTotalClassDto);
                    classNumber = getClassNumber(trialBalanceAccountDto);
                    subTotalClassDto = new TrialBalanceAccountDto();
                }
                setTrialBalanceSubTotal(subTotalClassDto, trialBalanceAccountDto);
                insertTrialBalanceAccountLineIntoReport(trialBalanceReport, trialBalanceAccountDto);
            }
            insertTrialBalanceSubTotalLineIntoReport(classNumber, subTotalClassDto, trialBalanceReport);
            setAccumulationInTrialBalanceResultIfClassIsProductOrExpense(trialBalanceResultDto, classNumber,
                    subTotalClassDto);
            setBalanceInTrialBalanceResult(trialBalanceResultDto);
            insertTrialBalanceResultLineIntoReport(trialBalanceResultDto, trialBalanceReport);
        }

        insertTrialBalanceTotalLineIntoReport(trialBalanceAccounts, trialBalanceReport);

        return trialBalanceReport;

    }

    private List<DocumentAccountLine> findAllDocumentAccountLineByCodesAndDates(LocalDateTime startDate,
            LocalDateTime endDate, String beginAccountCode, String endAccountCode) {
        int parseBeginAccountCode = getDefaultAccountCode(beginAccountCode, MIN_ACCOUNT_CODE);
        int parseEndAccountCode = getDefaultAccountCode(endAccountCode, GENERAL_LEDGER_MAX_ACCOUNT_CODE);
        return documentAccountLineDao.findAllDocumentAccountLinesInDatesAndAccountCodes(parseBeginAccountCode,
                parseEndAccountCode, startDate, endDate);
    }

    @Override
    public void insertTrialBalanceTotalLineIntoReport(List<TrialBalanceAccountDto> trialBalanceAccounts,
            List<TrialBalanceReportLineDto> trialBalanceReport) {

        BigDecimal totalInitialDebit = calculateTotalInitialDebitTrialBalance(trialBalanceAccounts);
        BigDecimal totalInitialCredit = calculateTotalInitialCreditTrialBalance(trialBalanceAccounts);
        BigDecimal totalCurrentDebit = calculateTotalCurrentDebitTrialBalance(trialBalanceAccounts);
        BigDecimal totalCurrentCredit = calculateTotalCurrentCreditTrialBalance(trialBalanceAccounts);
        BigDecimal accumulatedDebit = totalInitialDebit.add(totalCurrentDebit);
        BigDecimal accumulatedCredit = totalInitialCredit.add(totalCurrentCredit);
        BigDecimal balanceDebit = BigDecimal.ZERO;
        BigDecimal balanceCredit = BigDecimal.ZERO;

        if (accumulatedDebit.compareTo(accumulatedCredit) == ONE) {
            balanceDebit = accumulatedDebit.subtract(accumulatedCredit);
        } else {
            balanceCredit = accumulatedCredit.subtract(accumulatedDebit);
        }

        trialBalanceReport.add(new TrialBalanceReportLineDto(StringUtils.EMPTY, TRIAL_BALANCE_TOTAL,
                getFormattedBigDecimalValueOrEmptyStringIfZero(totalInitialDebit),
                getFormattedBigDecimalValueOrEmptyStringIfZero(totalInitialCredit),
                getFormattedBigDecimalValueOrEmptyStringIfZero(totalCurrentDebit),
                getFormattedBigDecimalValueOrEmptyStringIfZero(totalCurrentCredit),
                getFormattedBigDecimalValueOrEmptyStringIfZero(accumulatedDebit),
                getFormattedBigDecimalValueOrEmptyStringIfZero(accumulatedCredit),
                getAccountingDecimalFormat().format(balanceDebit), getAccountingDecimalFormat().format(balanceCredit)));
    }

    @Override
    public void insertTrialBalanceSubTotalLineIntoReport(int classNumber, TrialBalanceAccountDto subTotalClassDto,
            List<TrialBalanceReportLineDto> trialBalanceReport) {
        trialBalanceReport
                .add(new TrialBalanceReportLineDto(String.valueOf(classNumber), TOTAL_CLASS + " " + classNumber,
                        getFormattedBigDecimalValueOrEmptyStringIfZero(subTotalClassDto.getTotalInitialDebit()),
                        getFormattedBigDecimalValueOrEmptyStringIfZero(subTotalClassDto.getTotalInitialCredit()),
                        getFormattedBigDecimalValueOrEmptyStringIfZero(subTotalClassDto.getTotalCurrentDebit()),
                        getFormattedBigDecimalValueOrEmptyStringIfZero(subTotalClassDto.getTotalCurrentCredit()),
                        getFormattedBigDecimalValueOrEmptyStringIfZero(subTotalClassDto.getAccumulatedDebit()),
                        getFormattedBigDecimalValueOrEmptyStringIfZero(subTotalClassDto.getAccumulatedCredit()),
                        getFormattedBigDecimalValueOrEmptyStringIfZero(subTotalClassDto.getBalanceDebit()),
                        getFormattedBigDecimalValueOrEmptyStringIfZero(subTotalClassDto.getBalanceCredit())));
    }

    @Override
    public void insertTrialBalanceResultLineIntoReport(TrialBalanceAccountDto trialBalanceResultDto,
            List<TrialBalanceReportLineDto> trialBalanceReport) {
        trialBalanceReport.add(new TrialBalanceReportLineDto(StringUtils.EMPTY, RESULT, EMPTY_STRING, EMPTY_STRING,
                EMPTY_STRING, EMPTY_STRING,
                getFormattedBigDecimalValueOrEmptyStringIfZero(trialBalanceResultDto.getAccumulatedDebit()),
                getFormattedBigDecimalValueOrEmptyStringIfZero(trialBalanceResultDto.getAccumulatedCredit()),
                getFormattedBigDecimalValueOrEmptyStringIfZero(trialBalanceResultDto.getBalanceDebit()),
                getFormattedBigDecimalValueOrEmptyStringIfZero(trialBalanceResultDto.getBalanceCredit())));
    }

    @Override
    public void insertTrialBalanceAccountLineIntoReport(List<TrialBalanceReportLineDto> trialBalanceReport,
            TrialBalanceAccountDto trialBalanceAccountDto) {
        trialBalanceReport
                .add(new TrialBalanceReportLineDto(String.valueOf(trialBalanceAccountDto.getAccountDto().getCode()),
                        trialBalanceAccountDto.getAccountDto().getLabel(),
                        getFormattedBigDecimalValueOrEmptyStringIfZero(trialBalanceAccountDto.getTotalInitialDebit()),
                        getFormattedBigDecimalValueOrEmptyStringIfZero(trialBalanceAccountDto.getTotalInitialCredit()),
                        getFormattedBigDecimalValueOrEmptyStringIfZero(trialBalanceAccountDto.getTotalCurrentDebit()),
                        getFormattedBigDecimalValueOrEmptyStringIfZero(trialBalanceAccountDto.getTotalCurrentCredit()),
                        getFormattedBigDecimalValueOrEmptyStringIfZero(trialBalanceAccountDto.getAccumulatedDebit()),
                        getFormattedBigDecimalValueOrEmptyStringIfZero(trialBalanceAccountDto.getAccumulatedCredit()),
                        getFormattedBigDecimalValueOrEmptyStringIfZero(trialBalanceAccountDto.getBalanceDebit()),
                        getFormattedBigDecimalValueOrEmptyStringIfZero(trialBalanceAccountDto.getBalanceCredit())));
    }

    @Override
    public BigDecimal calculateTotalInitialCreditTrialBalance(List<TrialBalanceAccountDto> trialBalanceAccounts) {
        return trialBalanceAccounts.stream().map(TrialBalanceAccountDto::getTotalInitialCredit).reduce(BigDecimal.ZERO,
                BigDecimal::add);
    }

    @Override
    public BigDecimal calculateTotalInitialDebitTrialBalance(List<TrialBalanceAccountDto> trialBalanceAccounts) {
        return trialBalanceAccounts.stream().map(TrialBalanceAccountDto::getTotalInitialDebit).reduce(BigDecimal.ZERO,
                BigDecimal::add);
    }

    @Override
    public BigDecimal calculateTotalCurrentCreditTrialBalance(List<TrialBalanceAccountDto> trialBalanceAccounts) {
        return trialBalanceAccounts.stream().map(TrialBalanceAccountDto::getTotalCurrentCredit).reduce(BigDecimal.ZERO,
                BigDecimal::add);
    }

    @Override
    public BigDecimal calculateTotalCurrentDebitTrialBalance(List<TrialBalanceAccountDto> trialBalanceAccounts) {
        return trialBalanceAccounts.stream().map(TrialBalanceAccountDto::getTotalCurrentDebit).reduce(BigDecimal.ZERO,
                BigDecimal::add);
    }

}
