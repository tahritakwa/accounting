package fr.sparkit.accounting.dto;

import org.apache.commons.lang3.StringUtils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GeneralLedgerReportLineDto {

    private String accountCode = StringUtils.EMPTY;
    private String documentDate = StringUtils.EMPTY;
    private String label = StringUtils.EMPTY;
    private String documentCode = StringUtils.EMPTY;
    private String journal = StringUtils.EMPTY;
    private String debit = StringUtils.EMPTY;
    private String credit = StringUtils.EMPTY;
    private String balance = StringUtils.EMPTY;

    public GeneralLedgerReportLineDto(String accountCode) {
        this.accountCode = accountCode;
    }

    public GeneralLedgerReportLineDto(String documentDate, String label, String documentCode, String journal,
            String debit, String credit, String balance) {
        super();
        this.documentDate = documentDate;
        this.label = label;
        this.documentCode = documentCode;
        this.journal = journal;
        this.debit = debit;
        this.credit = credit;
        this.balance = balance;
    }

    public GeneralLedgerReportLineDto(String documentCode, String debit, String credit, String balance) {
        this.documentCode = documentCode;
        this.debit = debit;
        this.credit = credit;
        this.balance = balance;
    }

}
