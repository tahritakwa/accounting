package fr.sparkit.accounting.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class GeneralLedgerPageDto {

    private List<GeneralLedgerAccountDto> content;
    private Long totalElements;
}
