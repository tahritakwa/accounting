package fr.sparkit.accounting.convertor;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import fr.sparkit.accounting.dto.AccountDto;
import fr.sparkit.accounting.entities.Account;
import fr.sparkit.accounting.entities.ChartAccounts;

public final class AccountConvertor implements Serializable {

    private static final long serialVersionUID = 1L;

    private AccountConvertor() {
        super();
    }

    public static AccountDto modelToDto(Account account) {
        if (account == null) {
            return null;
        }
        return new AccountDto(account.getId(), account.getCode(), account.getLabel(), account.getPlan().getId(),
                account.getPlan().getCode(), account.getDebitOpening(), account.getCreditOpening(),
                account.isLiterable(), account.isReconcilable(), account.getTiersId(), account.isDeleted());
    }

    public static Account dtoToModel(AccountDto accountDto, ChartAccounts chartAccount) {
        return new Account(accountDto.getId(), accountDto.getCode(), accountDto.getLabel(), chartAccount,
                accountDto.getDebitOpening(), accountDto.getCreditOpening(), accountDto.isLiterable(),
                accountDto.isReconcilable(), accountDto.getTiersId(), false, null);
    }

    public static List<AccountDto> modelsToDtos(Collection<Account> accounts) {
        return accounts.stream().filter(Objects::nonNull).map(AccountConvertor::modelToDto)
                .collect(Collectors.toList());
    }

}
