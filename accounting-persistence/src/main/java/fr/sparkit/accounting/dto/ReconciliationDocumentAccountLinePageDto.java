package fr.sparkit.accounting.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ReconciliationDocumentAccountLinePageDto {

    private List<CloseDocumentAccountLineDto> closeDocumentAccountLineDtos;
    private Long total;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;

}
