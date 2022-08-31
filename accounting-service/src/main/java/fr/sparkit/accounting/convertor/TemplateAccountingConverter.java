package fr.sparkit.accounting.convertor;

import java.util.List;

import fr.sparkit.accounting.dto.TemplateAccountingDetailsDto;
import fr.sparkit.accounting.dto.TemplateAccountingDto;
import fr.sparkit.accounting.entities.Journal;
import fr.sparkit.accounting.entities.TemplateAccounting;

public final class TemplateAccountingConverter {

    private TemplateAccountingConverter() {
        super();

    }

    public static TemplateAccountingDto modelToDto(TemplateAccounting templateAccounting,
                                                   List<TemplateAccountingDetailsDto> templateAccountingDetailsDtos) {
        return new TemplateAccountingDto(templateAccounting.getId(), templateAccounting.getLabel(),
                templateAccounting.getJournal().getId(), templateAccounting.getJournal().getLabel(),
                templateAccountingDetailsDtos);
    }

    public static TemplateAccounting dtoToModel(TemplateAccountingDto templateAccountingDto, Journal journal) {
        return new TemplateAccounting(templateAccountingDto.getId(), templateAccountingDto.getLabel(), journal, false,
                null);
    }


    public static TemplateAccountingDto modelToDto(TemplateAccounting templateAccounting) {
        return new TemplateAccountingDto(templateAccounting.getId(), templateAccounting.getLabel(),
                templateAccounting.getJournal().getId(), templateAccounting.getJournal().getLabel(),
                null);
    }

}
