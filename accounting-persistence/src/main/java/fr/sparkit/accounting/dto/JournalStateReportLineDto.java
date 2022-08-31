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
public class JournalStateReportLineDto {
    private String journalLabel = StringUtils.EMPTY;
    private String reference = StringUtils.EMPTY;
    private String code = StringUtils.EMPTY;
    private String date = StringUtils.EMPTY;
    private String totalAmount = StringUtils.EMPTY;

    public JournalStateReportLineDto(String journalLabel) {
        this.journalLabel = journalLabel;
    }

    public JournalStateReportLineDto(String reference, String totalAmount) {
        this.reference = reference;
        this.totalAmount = totalAmount;
    }
}
