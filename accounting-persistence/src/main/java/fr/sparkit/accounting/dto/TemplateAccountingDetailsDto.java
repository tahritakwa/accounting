package fr.sparkit.accounting.dto;

import java.math.BigDecimal;

import javax.validation.constraints.Size;

import fr.sparkit.accounting.constants.AccountingConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TemplateAccountingDetailsDto {
    private Long id;
    private Long accountId;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    @Size(min = AccountingConstants.ENTITY_DEFAULT_LABEL_MIN_LENGTH, max = AccountingConstants.ENTITY_DEFAULT_LABEL_MAX_LENGTH)
    private String label;

}
