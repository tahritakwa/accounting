package fr.sparkit.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonFormat;

import fr.sparkit.accounting.constants.AccountingConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentAccountingDto {

    private Long id;
    @JsonFormat(pattern = "yyyy/MM/dd HH:mm:ss")
    private LocalDateTime documentDate;
    @Size(min = AccountingConstants.ENTITY_DEFAULT_LABEL_MIN_LENGTH, max = AccountingConstants.ENTITY_DEFAULT_LABEL_MAX_LENGTH)
    private String label;
    private String codeDocument;
    private BigDecimal totalDebitAmount;
    private BigDecimal totalCreditAmount;
    private Long journalId;
    @Size(min = AccountingConstants.ENTITY_DEFAULT_LABEL_MIN_LENGTH, max = AccountingConstants.ENTITY_DEFAULT_LABEL_MAX_LENGTH)
    private String journalLabel;
    private List<DocumentAccountLineDto> documentAccountLines;
    private Long fiscalYearId;
    private Long billId;
    private int indexOfStatus;

    public DocumentAccountingDto(String label, LocalDateTime documentDate, Long journalId, int indexOfStatus,
            List<DocumentAccountLineDto> documentAccountLines) {
        this.documentDate = documentDate;
        this.journalId = journalId;
        this.label = label;
        this.indexOfStatus = indexOfStatus;
        this.documentAccountLines = new ArrayList<>(documentAccountLines);
    }

    public DocumentAccountingDto(Long id, BigDecimal totalDebitAmount, BigDecimal totalCreditAmount) {
        this.id = id;
        this.totalDebitAmount = totalDebitAmount;
        this.totalCreditAmount = totalCreditAmount;
    }

    public DocumentAccountingDto(List<DocumentAccountLineDto> documentAccountLines) {
        this.documentAccountLines = new ArrayList<>(documentAccountLines);
    }

    public DocumentAccountingDto(String codeDocument) {
        super();
        this.codeDocument = codeDocument;
    }

    public DocumentAccountingDto(LocalDateTime documentDate, @Size(min = 2, max = 255) String label, Long journalId,
            List<DocumentAccountLineDto> documentAccountLines, Long fiscalYearId) {
        super();
        this.documentDate = documentDate;
        this.label = label;
        this.journalId = journalId;
        this.documentAccountLines =  new ArrayList<>(documentAccountLines);
        this.fiscalYearId = fiscalYearId;
    }

}
