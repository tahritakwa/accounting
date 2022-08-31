package fr.sparkit.accounting.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class TrialBalancePageDto {

    private List<TrialBalanceAccountDto> content;
    private Long totalElements;
}
