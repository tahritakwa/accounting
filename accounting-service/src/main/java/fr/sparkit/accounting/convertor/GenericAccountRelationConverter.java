package fr.sparkit.accounting.convertor;

import fr.sparkit.accounting.dto.GenericAccountRelationDto;
import fr.sparkit.accounting.entities.account.relations.GenericAccountRelation;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class GenericAccountRelationConverter {

    private GenericAccountRelationConverter() {
        super();
    }

    public static GenericAccountRelationDto modelToDto(GenericAccountRelation accountRelation) {
        if (accountRelation == null) {
            return null;
        }
        return new GenericAccountRelationDto(accountRelation.getId(), accountRelation.getAccount().getId(), accountRelation.getRelationEntityId());
    }

    public static List<GenericAccountRelationDto> modelsToDtos(Collection<GenericAccountRelation> accountRelations) {
        return accountRelations.stream().filter(Objects::nonNull).map(GenericAccountRelationConverter::modelToDto)
                .collect(Collectors.toList());
    }
}
