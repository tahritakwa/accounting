package fr.sparkit.accounting.dto;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CentralizingJournalDto {

    private Page<JournalDto> journalPage;
    private BigDecimal totalDebitAmount;
    private BigDecimal totalCreditAmount;
}
