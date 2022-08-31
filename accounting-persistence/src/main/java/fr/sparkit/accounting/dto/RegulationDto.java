package fr.sparkit.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RegulationDto {

    private Long idSettlement;
    private String codeSettlement;
    private boolean isAccounted;
    private BigDecimal paymentAmount;
    private BigDecimal withHoldingTax;
    private Long tierId;
    private String tiersName;
    @JsonFormat(pattern = "yyyy/MM/dd HH:mm:ss")
    private LocalDateTime settlementDate;
    private Long bankId;
    private String bankName;
    private String paymentMethodeName;
    private Long idJournal;
    private String documentType;
    private List<DocumentSettlementAccountingDetails> documentSettlementAccountingDetails;
    private List<RistournSettlementAccountingDetails> ristournSettlementAccountingDetails;

}
