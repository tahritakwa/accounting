package fr.sparkit.accounting.services.impl.relations;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.sparkit.accounting.convertor.GenericAccountRelationConverter;
import fr.sparkit.accounting.dao.AccountWithHoldingTaxDao;
import fr.sparkit.accounting.dto.GenericAccountRelationDto;
import fr.sparkit.accounting.entities.Account;
import fr.sparkit.accounting.entities.account.relations.AccountWithHoldingTax;
import fr.sparkit.accounting.entities.account.relations.GenericAccountRelation;
import fr.sparkit.accounting.enumuration.AccountRelationType;
import fr.sparkit.accounting.services.IGenericAccountRelationService;
import fr.sparkit.accounting.services.impl.AccountService;
import fr.sparkit.accounting.services.impl.GenericService;
import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.errors.ErrorsResponse;
import fr.sparkit.accounting.util.http.HttpCustomException;

@Service("AccountWithHoldingTaxService")
public class AccountWithHoldingTaxService extends GenericService<AccountWithHoldingTax, Long>
        implements IGenericAccountRelationService {
    private final AccountService accountService;
    private final AccountWithHoldingTaxDao accountWithHoldingTaxDao;

    @Autowired
    public AccountWithHoldingTaxService(AccountService accountService,
            AccountWithHoldingTaxDao accountWithHoldingTaxDao) {
        this.accountService = accountService;
        this.accountWithHoldingTaxDao = accountWithHoldingTaxDao;
    }

    @Override
    public GenericAccountRelationDto saveAccountRelation(GenericAccountRelationDto accountRelationDto) {
        Account account = accountService.findOne(accountRelationDto.getAccountId());
        GenericAccountRelation existingAccountWithRelation = findAccountWithRelation(
                accountRelationDto.getRelationEntityId()).orElse(null);
        if (existingAccountWithRelation != null
                && !existingAccountWithRelation.getId().equals(accountRelationDto.getId())) {
            throw new HttpCustomException(ApiErrors.Accounting.ACCOUNT_RELATION_TYPE_DUPLICATES, new ErrorsResponse()
                    .error(accountRelationDto.getRelationEntityId()).error(AccountRelationType.SUPPLIER));
        } else {
            if (account != null) {
                return GenericAccountRelationConverter
                        .modelToDto(saveAndFlush(new AccountWithHoldingTax(accountRelationDto.getId(), account,
                                accountRelationDto.getRelationEntityId())));
            } else {
                throw new HttpCustomException(ApiErrors.Accounting.ACCOUNT_DOES_NOT_EXIST_ALLOCATION_NOT_POSSIBLE,
                        new ErrorsResponse().error(accountRelationDto.getAccountId()));
            }
        }
    }

    @Override
    public Optional<GenericAccountRelation> findAccountWithRelation(Long relationEntityId) {
        return accountWithHoldingTaxDao.findByRelationEntityIdAndIsDeletedFalse(relationEntityId);
    }

    @Override
    public List<GenericAccountRelationDto> findAllEntitiesInRelationWithAccount(Long accountId) {
        return GenericAccountRelationConverter
                .modelsToDtos(accountWithHoldingTaxDao.findAllByAccountIdAndIsDeletedFalse(accountId));
    }

    @Override
    public void deleteAccountRelation(Long entityRelationId) {
        findAccountWithRelation(entityRelationId).ifPresent(accountRelation -> delete(accountRelation.getId()));
    }
}
