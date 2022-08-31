package fr.sparkit.accounting.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CloseAndReopeningFiscalYearDto {
    private Long currentFiscalYear;
    private Long targetFiscalYearId;
    private boolean transferOfDepreciationPeriod;
    private boolean transferOfReports;
    private boolean passEntryAccounting;
    private Long journalANewId;
    private Long resultAccount;
    private boolean literableAccounts;
}
