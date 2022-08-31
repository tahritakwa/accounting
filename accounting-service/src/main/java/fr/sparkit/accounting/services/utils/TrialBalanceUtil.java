package fr.sparkit.accounting.services.utils;

import static fr.sparkit.accounting.constants.NumberConstant.ONE;

import java.math.BigDecimal;
import java.util.Collection;

import fr.sparkit.accounting.constants.AccountingConstants;
import fr.sparkit.accounting.dto.AccountBalanceDto;
import fr.sparkit.accounting.dto.AccountDto;
import fr.sparkit.accounting.dto.InitialTrialBalanceDto;
import fr.sparkit.accounting.dto.TrialBalanceAccountAmountDto;
import fr.sparkit.accounting.dto.TrialBalanceAccountDto;
import fr.sparkit.accounting.enumuration.DocumentAccountStatus;

public final class TrialBalanceUtil {

    private TrialBalanceUtil() {
        super();
    }

    public static int getClassNumber(TrialBalanceAccountDto trialBalanceAccountDto) {
        return trialBalanceAccountDto.getAccountDto().getCode() / AccountingConstants.MIN_ACCOUNT_CODE;
    }

    public static boolean isTrialBalanceAccountNotInCurrentClassNumber(int classNumber,
            TrialBalanceAccountDto trialBalanceAccountDto) {
        return getClassNumber(trialBalanceAccountDto) != classNumber;
    }

    public static boolean isLastPage(int page, int size, long totalElements) {
        return ((page * size) + ONE <= totalElements && totalElements <= (page + ONE) * size);
    }

    public static void setBalanceInTrialBalanceResult(TrialBalanceAccountDto trialBalanceResultDto) {
        if (trialBalanceResultDto.getAccumulatedDebit()
                .compareTo(trialBalanceResultDto.getAccumulatedCredit()) == ONE) {
            trialBalanceResultDto.setBalanceDebit(
                    trialBalanceResultDto.getAccumulatedDebit().subtract(trialBalanceResultDto.getAccumulatedCredit()));
        } else {
            trialBalanceResultDto.setBalanceCredit(
                    trialBalanceResultDto.getAccumulatedCredit().subtract(trialBalanceResultDto.getAccumulatedDebit()));
        }
    }

    public static void setAccumulationInTrialBalanceResultIfClassIsProductOrExpense(
            TrialBalanceAccountDto trialBalanceResultDto, int classNumber, TrialBalanceAccountDto subTotalClassDto) {
        if (classNumber == AccountingConstants.REVENUE_ACCOUNTS
                || classNumber == AccountingConstants.EXPENSE_ACCOUNTS) {
            trialBalanceResultDto.setAccumulatedDebit(
                    trialBalanceResultDto.getAccumulatedDebit().add(subTotalClassDto.getAccumulatedDebit()));
            trialBalanceResultDto.setAccumulatedCredit(
                    trialBalanceResultDto.getAccumulatedCredit().add(subTotalClassDto.getAccumulatedCredit()));
        }
    }

    public static void addTotalAmountsToTrialBalanceAccounts(Collection<TrialBalanceAccountDto> trialBalanceAccounts,
            AccountDto accountDto, Collection<TrialBalanceAccountAmountDto> trialBalanceAmountsDtos) {

        BigDecimal initialDebit = trialBalanceAmountsDtos.stream()
                .filter((TrialBalanceAccountAmountDto trialBalanceAccountAmountDto) -> trialBalanceAccountAmountDto
                        .getIndexOfDocumentAccountStatusToGroupBy() == DocumentAccountStatus.BY_CONCLUDING_CURRENT_FISCAL_YEAR_IS_CREATED
                                .getIndex())
                .map(TrialBalanceAccountAmountDto::getTotalDebit).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal initialCredit = trialBalanceAmountsDtos.stream()
                .filter((TrialBalanceAccountAmountDto trialBalanceAccountAmountDto) -> trialBalanceAccountAmountDto
                        .getIndexOfDocumentAccountStatusToGroupBy() == DocumentAccountStatus.BY_CONCLUDING_CURRENT_FISCAL_YEAR_IS_CREATED
                                .getIndex())
                .map(TrialBalanceAccountAmountDto::getTotalCredit).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal currentDebit = trialBalanceAmountsDtos.stream()
                .filter((TrialBalanceAccountAmountDto trialBalanceAccountAmountDto) -> trialBalanceAccountAmountDto
                        .getIndexOfDocumentAccountStatusToGroupBy() != DocumentAccountStatus.BY_CONCLUDING_CURRENT_FISCAL_YEAR_IS_CREATED
                                .getIndex())
                .map(TrialBalanceAccountAmountDto::getTotalDebit).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal currentCredit = trialBalanceAmountsDtos.stream()
                .filter((TrialBalanceAccountAmountDto trialBalanceAccountAmountDto) -> trialBalanceAccountAmountDto
                        .getIndexOfDocumentAccountStatusToGroupBy() != DocumentAccountStatus.BY_CONCLUDING_CURRENT_FISCAL_YEAR_IS_CREATED
                                .getIndex())
                .map(TrialBalanceAccountAmountDto::getTotalCredit).reduce(BigDecimal.ZERO, BigDecimal::add);

        TrialBalanceAccountDto trialBalanceAccountDto = convertAccountToTrialBalanceAccounts(accountDto, initialDebit,
                initialCredit, currentDebit, currentCredit);

        trialBalanceAccounts.add(trialBalanceAccountDto);
    }

    private static TrialBalanceAccountDto convertAccountToTrialBalanceAccounts(AccountDto accountDto,
            BigDecimal initialDebit, BigDecimal initialCredit, BigDecimal currentDebit, BigDecimal currentCredit) {
        BigDecimal accumulatedDebit = initialDebit.add(currentDebit);
        BigDecimal accumulatedCredit = initialCredit.add(currentCredit);

        BigDecimal balanceDebit = BigDecimal.ZERO;
        BigDecimal balanceCredit = BigDecimal.ZERO;

        if (accumulatedDebit.compareTo(accumulatedCredit) == ONE) {
            balanceDebit = accumulatedDebit.subtract(accumulatedCredit);
        } else {
            balanceCredit = accumulatedCredit.subtract(accumulatedDebit);
        }

        return new TrialBalanceAccountDto(accountDto, initialDebit, initialCredit, currentDebit, currentCredit,
                accumulatedDebit, accumulatedCredit, balanceDebit, balanceCredit);
    }

    public static void setTrialBalanceSubTotal(TrialBalanceAccountDto subTotalClassDto,
            TrialBalanceAccountDto trialBalanceAccountDto) {
        subTotalClassDto.setTotalInitialDebit(
                subTotalClassDto.getTotalInitialDebit().add(trialBalanceAccountDto.getTotalInitialDebit()));
        subTotalClassDto.setTotalInitialCredit(
                subTotalClassDto.getTotalInitialCredit().add(trialBalanceAccountDto.getTotalInitialCredit()));
        subTotalClassDto.setTotalCurrentDebit(
                subTotalClassDto.getTotalCurrentDebit().add(trialBalanceAccountDto.getTotalCurrentDebit()));
        subTotalClassDto.setTotalCurrentCredit(
                subTotalClassDto.getTotalCurrentCredit().add(trialBalanceAccountDto.getTotalCurrentCredit()));
        subTotalClassDto.setAccumulatedDebit(
                subTotalClassDto.getAccumulatedDebit().add(trialBalanceAccountDto.getAccumulatedDebit()));
        subTotalClassDto.setAccumulatedCredit(
                subTotalClassDto.getAccumulatedCredit().add(trialBalanceAccountDto.getAccumulatedCredit()));

        BigDecimal accumulatedDebit = subTotalClassDto.getBalanceDebit().add(trialBalanceAccountDto.getBalanceDebit());
        BigDecimal accumulatedCredit = subTotalClassDto.getBalanceCredit()
                .add(trialBalanceAccountDto.getBalanceCredit());

        if (accumulatedDebit.compareTo(accumulatedCredit) == ONE) {
            subTotalClassDto.setBalanceDebit(accumulatedDebit.subtract(accumulatedCredit));
            subTotalClassDto.setBalanceCredit(BigDecimal.ZERO);
        } else {
            subTotalClassDto.setBalanceCredit(accumulatedCredit.subtract(accumulatedDebit));
            subTotalClassDto.setBalanceDebit(BigDecimal.ZERO);
        }
    }

    public static void addAllToTrialBalanceAccounts(Iterable<AccountDto> accountDtos,
            Collection<InitialTrialBalanceDto> initialTrialBalanceDtos,
            Collection<AccountBalanceDto> currentTrialBalanceDtos,
            Collection<TrialBalanceAccountDto> trialBalanceAccounts) {
        for (AccountDto accountDto : accountDtos) {
            InitialTrialBalanceDto initialTrialBalanceDto = initialTrialBalanceDtos.stream()
                    .filter(element -> element.getAccountId().equals(accountDto.getId())).findAny().orElse(null);

            BigDecimal totalInitialDebit = BigDecimal.ZERO;
            BigDecimal totalInitialCredit = BigDecimal.ZERO;
            if (initialTrialBalanceDto != null) {
                totalInitialDebit = initialTrialBalanceDto.getTotalInitialDebit();
                totalInitialCredit = initialTrialBalanceDto.getTotalInitialCredit();
            }

            AccountBalanceDto currentTrialBalanceDto = currentTrialBalanceDtos.stream()
                    .filter(element -> element.getAccountId().equals(accountDto.getId())).findAny().orElse(null);

            BigDecimal totalCurrentDebit = BigDecimal.ZERO;
            BigDecimal totalCurrentCredit = BigDecimal.ZERO;
            if (currentTrialBalanceDto != null) {
                totalCurrentDebit = currentTrialBalanceDto.getTotalCurrentDebit();
                totalCurrentCredit = currentTrialBalanceDto.getTotalCurrentCredit();
            }

            TrialBalanceAccountDto trialBalanceAccountDto = convertAccountToTrialBalanceAccounts(accountDto,
                    totalInitialDebit, totalInitialCredit, totalCurrentDebit, totalCurrentCredit);

            trialBalanceAccounts.add(trialBalanceAccountDto);
        }
    }
}
