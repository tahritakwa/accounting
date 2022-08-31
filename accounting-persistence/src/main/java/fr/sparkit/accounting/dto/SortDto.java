package fr.sparkit.accounting.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SortDto {
    private String field;
    private org.springframework.data.domain.Sort.Direction direction;

    public boolean areAllFieldsNotNull() {
        return (field != null && direction != null);
    }
}
