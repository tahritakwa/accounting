package fr.sparkit.accounting.dto;

import java.math.BigDecimal;
import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CentralizingJournalDetailsByMonthDto implements Comparable<CentralizingJournalDetailsByMonthDto> {

    private Integer planCode;
    private String planLabel;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;

    public CentralizingJournalDetailsByMonthDto(Integer planCode, BigDecimal debitAmount, BigDecimal creditAmount) {
        super();
        this.planCode = planCode;
        this.debitAmount = debitAmount;
        this.creditAmount = creditAmount;
    }

    @Override
    public int compareTo(CentralizingJournalDetailsByMonthDto centralizingJournalDetailsByMonthDto) {
        return this.getPlanCode().compareTo(centralizingJournalDetailsByMonthDto.getPlanCode());
    }

    @Override
    public boolean equals(Object centralizingJournalDetailsDtoObject) {
        if (this == centralizingJournalDetailsDtoObject) {
            return true;
        }
        if (centralizingJournalDetailsDtoObject == null
                || getClass() != centralizingJournalDetailsDtoObject.getClass()) {
            return false;
        }
        CentralizingJournalDetailsByMonthDto centralizingJournalDetailsByMonthDto = (CentralizingJournalDetailsByMonthDto) centralizingJournalDetailsDtoObject;
        return Objects.equals(planCode, centralizingJournalDetailsByMonthDto.planCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(planCode);
    }
}
