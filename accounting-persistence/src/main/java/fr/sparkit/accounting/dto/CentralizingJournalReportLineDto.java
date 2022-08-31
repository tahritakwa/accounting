package fr.sparkit.accounting.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CentralizingJournalReportLineDto {

    private String journalCode;
    private String journalLabel;
    private String accountCode;
    private String accountLabel;
    private String totalDebitAmount;
    private String totalCreditAmount;

}
