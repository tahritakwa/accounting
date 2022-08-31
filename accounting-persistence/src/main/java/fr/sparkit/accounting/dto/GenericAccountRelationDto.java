package fr.sparkit.accounting.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.Min;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class GenericAccountRelationDto {
    private Long id;
    @Min(0)
    private Long accountId;
    @Min(0)
    private Long relationEntityId;
}
