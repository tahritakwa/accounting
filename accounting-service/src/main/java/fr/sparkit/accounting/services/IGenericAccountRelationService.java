package fr.sparkit.accounting.services;

import java.util.List;
import java.util.Optional;

import fr.sparkit.accounting.dto.GenericAccountRelationDto;
import fr.sparkit.accounting.entities.account.relations.GenericAccountRelation;

public interface IGenericAccountRelationService {

    GenericAccountRelationDto saveAccountRelation(GenericAccountRelationDto accountRelationDto);

    Optional<GenericAccountRelation> findAccountWithRelation(Long relationEntityId);

    List<GenericAccountRelationDto> findAllEntitiesInRelationWithAccount(Long accountId);

    void deleteAccountRelation(Long entityRelationId);
}
