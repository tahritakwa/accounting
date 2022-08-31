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
public class AuxiliaryJournalLineDto {

    private String journal = StringUtils.EMPTY;
    private String documentDate = StringUtils.EMPTY;
    private String code = StringUtils.EMPTY;
    private String label = StringUtils.EMPTY;
    private String dateDocumentLineDate = StringUtils.EMPTY;
    private String accountCode = StringUtils.EMPTY;
    private String debit = StringUtils.EMPTY;
    private String credit = StringUtils.EMPTY;

    public AuxiliaryJournalLineDto(String documentDate, String code, String label, String dateDocumentLineDate,
            String accountCode, String debit, String credit) {
        this.documentDate = documentDate;
        this.code = code;
        this.dateDocumentLineDate = dateDocumentLineDate;
        this.accountCode = accountCode;
        this.label = label;
        this.debit = debit;
        this.credit = credit;
    }

    public AuxiliaryJournalLineDto(String label, String debit, String credit) {
        this.label = label;
        this.debit = debit;
        this.credit = credit;
    }

    public AuxiliaryJournalLineDto(String journal) {
        this.journal = journal;
    }

}
