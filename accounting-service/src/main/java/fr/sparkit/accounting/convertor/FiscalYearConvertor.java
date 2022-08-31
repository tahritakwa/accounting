package fr.sparkit.accounting.convertor;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.persistence.Converter;

import fr.sparkit.accounting.dto.FiscalYearDto;
import fr.sparkit.accounting.entities.FiscalYear;

@Converter
public final class FiscalYearConvertor {
    private FiscalYearConvertor() {
    }

    public static FiscalYearDto modelToDto(FiscalYear fiscalYear) {
        if (fiscalYear == null) {
            return null;
        }
        return new FiscalYearDto(fiscalYear.getId(), fiscalYear.getName(), fiscalYear.getStartDate(),
                fiscalYear.getEndDate(), fiscalYear.getClosingDate(), fiscalYear.getConclusionDate(),
                fiscalYear.getClosingState());
    }

    public static FiscalYear dtoToModel(FiscalYearDto fiscalYearDto) {
        return new FiscalYear(fiscalYearDto.getId(), fiscalYearDto.getName(), fiscalYearDto.getStartDate(),
                fiscalYearDto.getEndDate(), fiscalYearDto.getClosingDate(), fiscalYearDto.getConclusionDate(),
                fiscalYearDto.getClosingState(), false, null);
    }

    public static List<FiscalYearDto> modelsToDtos(Collection<FiscalYear> models) {
        return models.stream().filter(Objects::nonNull).map(FiscalYearConvertor::modelToDto)
                .collect(Collectors.toList());
    }
}
