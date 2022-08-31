package fr.sparkit.accounting.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import fr.sparkit.accounting.entities.account.relations.AccountBank;
import fr.sparkit.accounting.entities.account.relations.GenericAccountRelation;

@Repository
public interface AccountBankDao extends BaseRepository<AccountBank, Long> {

    List<GenericAccountRelation> findAllByAccountIdAndIsDeletedFalse(Long accountId);

    Optional<GenericAccountRelation> findByRelationEntityIdAndIsDeletedFalse(Long bankId);
}
