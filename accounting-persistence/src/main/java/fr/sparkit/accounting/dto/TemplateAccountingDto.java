package fr.sparkit.accounting.dto;

import java.util.List;

import javax.validation.Valid;
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
public class TemplateAccountingDto {
    private Long id;
    @Size(min = AccountingConstants.ENTITY_DEFAULT_LABEL_MIN_LENGTH, max = AccountingConstants.ENTITY_DEFAULT_LABEL_MAX_LENGTH)
    private String label;
    private Long journalId;
    @Size(min = AccountingConstants.ENTITY_DEFAULT_LABEL_MIN_LENGTH, max = AccountingConstants.ENTITY_DEFAULT_LABEL_MAX_LENGTH)
    private String journalLabel;
    @Valid
    private List<TemplateAccountingDetailsDto> templateAccountingDetails;

}
