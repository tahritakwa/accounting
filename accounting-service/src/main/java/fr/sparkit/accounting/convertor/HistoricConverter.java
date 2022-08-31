package fr.sparkit.accounting.convertor;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;

import fr.sparkit.accounting.auditing.HistoricActionEnum;
import fr.sparkit.accounting.dto.HistoricDto;
import fr.sparkit.accounting.dto.HistoricDtoPage;
import fr.sparkit.accounting.entities.Historic;


public final class HistoricConverter {


    private HistoricConverter() {

    }

    public static HistoricDto modelToDto(Historic historic) {
        return new HistoricDto(historic.getCreatedBy(), historic.getCreatedDate(), historic.getLastModifiedBy(),
                historic.getLastModifiedDate(), historic.getId(), HistoricActionEnum.valueOf(historic.getAction()),
                historic.getEntity(), historic.getEntityId(), historic.getEntityField(), historic.getFieldOldValue(),
                historic.getFieldNewValue());
    }

    public static Historic dtoToModel(HistoricDto historicDto) {
        return new Historic(historicDto.getAction().toString(), historicDto.getEntity(), historicDto.getEntityId(),
                historicDto.getEntityField(), historicDto.getFieldOldValue(), historicDto.getFieldNewValue(),
                historicDto.getCreatedDate(), historicDto.getCreatedBy());
    }

    public static List<HistoricDto> modelsToDtos(Collection<Historic> historics) {
        return historics.stream().filter(Objects::nonNull).map(HistoricConverter::modelToDto)
                .collect(Collectors.toList());
    }

    public static HistoricDtoPage pageModelToPageDto(Page<Historic> historicPage) {
        return new HistoricDtoPage(
                historicPage.getContent().stream().map(HistoricConverter::modelToDto).collect(Collectors.toList()),
                historicPage.getTotalElements());
    }
}
