package fr.sparkit.accounting.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SettlementsToImportDto {

    private List<RegulationDto> regulationsDtos;
    private Long fiscalYearId;
    private Long cofferAccountId;
    private Long bankAccountId;
}
