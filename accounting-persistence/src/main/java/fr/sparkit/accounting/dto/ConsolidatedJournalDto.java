package fr.sparkit.accounting.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ConsolidatedJournalDto {
    private int code;
    private String label;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
}
