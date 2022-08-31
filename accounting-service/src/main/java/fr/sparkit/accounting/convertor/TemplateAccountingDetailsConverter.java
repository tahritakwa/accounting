package fr.sparkit.accounting.convertor;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import fr.sparkit.accounting.dto.TemplateAccountingDetailsDto;
import fr.sparkit.accounting.entities.TemplateAccountingDetails;

public final class TemplateAccountingDetailsConverter {

    private TemplateAccountingDetailsConverter() {
        super();
    }

    public static TemplateAccountingDetails dtoToModel(TemplateAccountingDetailsDto templateAccountingDetailsDto) {
        return new TemplateAccountingDetails(templateAccountingDetailsDto.getId(),
                templateAccountingDetailsDto.getLabel(), templateAccountingDetailsDto.getDebitAmount(),
                templateAccountingDetailsDto.getCreditAmount(), false, null);
    }

    public static TemplateAccountingDetailsDto modelToDto(TemplateAccountingDetails templateAccountingDetails) {
        return new TemplateAccountingDetailsDto(templateAccountingDetails.getId(),
                templateAccountingDetails.getAccount().getId(), templateAccountingDetails.getDebitAmount(),
                templateAccountingDetails.getCreditAmount(), templateAccountingDetails.getLabel());
    }

    public static List<TemplateAccountingDetailsDto> modelsToDtos(
            Collection<TemplateAccountingDetails> templateAccountingDetails) {
        return templateAccountingDetails.stream().filter(Objects::nonNull)
                .map(TemplateAccountingDetailsConverter::modelToDto).collect(Collectors.toList());
    }

}
