package fr.sparkit.accounting.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LiterableDocumentAccountLinePageDto {
    private List<LiterableDocumentAccountLineDto> content;
    private long totalElementsOfAccounts;
    private long totalElementsOfDocumentAccountLinesPerAccount;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
}
