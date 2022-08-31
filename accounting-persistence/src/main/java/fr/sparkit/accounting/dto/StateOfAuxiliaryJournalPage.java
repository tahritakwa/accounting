package fr.sparkit.accounting.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StateOfAuxiliaryJournalPage {

    private List<AuxiliaryJournalDto> content;
    private BigDecimal totalDebitAmount;
    private BigDecimal totalCreditAmount;
    private int totalElements;
}
