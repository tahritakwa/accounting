package fr.sparkit.accounting.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class GeneralLedgerDetailsPageDto {

    private List<GeneralLedgerAccountDetailsDto> content;
    private Long totalElements;
}
