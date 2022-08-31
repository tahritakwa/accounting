package fr.sparkit.accounting.services.utils;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import fr.sparkit.accounting.dto.AccountDto;
import fr.sparkit.accounting.dto.AccountBalanceDto;
import fr.sparkit.accounting.dto.GeneralLedgerAccountDto;

public final class GeneralLedgerUtil {

    private GeneralLedgerUtil() {
        super();
    }

    public static Consumer<AccountDto> generateGeneralLedger(List<GeneralLedgerAccountDto> generalLedgerAccountDtos,
            Collection<AccountBalanceDto> currentTrialBalanceDtos) {
        return (AccountDto accountDto) -> {
            BigDecimal totalDebit = BigDecimal.ZERO;
            BigDecimal totalCredit = BigDecimal.ZERO;

            List<AccountBalanceDto> currentTrialBalance = currentTrialBalanceDtos.stream()
                    .filter(currentTrialBalanceDto -> currentTrialBalanceDto.getAccountId().equals(accountDto.getId()))
                    .collect(Collectors.toList());
            insertIntoAccountDtos(generalLedgerAccountDtos, accountDto, totalDebit, totalCredit, currentTrialBalance);
        };
    }

    public static void insertIntoAccountDtos(Collection<GeneralLedgerAccountDto> generalLedgerAccountDtos,
            AccountDto accountDto, BigDecimal totalDebit, BigDecimal totalCredit,
            List<AccountBalanceDto> currentTrialBalance) {
        if (!currentTrialBalance.isEmpty()) {
            totalDebit = currentTrialBalance.get(0).getTotalCurrentDebit();
            totalCredit = currentTrialBalance.get(0).getTotalCurrentCredit();
        }
        generalLedgerAccountDtos.add(new GeneralLedgerAccountDto(accountDto.getId(), accountDto.getCode(),
                accountDto.getLabel(), totalDebit, totalCredit, totalDebit.subtract(totalCredit), accountDto.isLiterable()));
    }
}
