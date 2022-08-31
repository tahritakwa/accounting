package fr.sparkit.accounting.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@AllArgsConstructor
public class HistoricDtoPage {
    private List<HistoricDto> historicDtoList = new ArrayList<>();
    private Long totalElements;
}
