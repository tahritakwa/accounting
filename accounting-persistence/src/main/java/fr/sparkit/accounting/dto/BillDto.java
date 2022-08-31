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
public class BillDto {

    private Long idDocument;
    private String codeDocument;
    private String documentType;
    private BigDecimal amountTTC;
    private Long tierId;
    private String tierName;
    private boolean isAccounted;
    private BigDecimal taxStamp;
    private List<BillDetailsDto> billDetails;
    @JsonFormat(pattern = "yyyy/MM/dd HH:mm:ss")
    private LocalDateTime documentDate;
    private Long idJournal;
    private BigDecimal ristourn;
}
