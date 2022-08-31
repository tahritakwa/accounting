package fr.sparkit.accounting.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class DocumentPageDto {

    private List<DocumentAccountingDto> listDocumentDto;
    private Long total;

}
