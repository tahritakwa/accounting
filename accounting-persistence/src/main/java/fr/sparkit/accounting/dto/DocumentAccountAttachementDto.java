package fr.sparkit.accounting.dto;

import fr.sparkit.accounting.entities.DocumentAccount;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class DocumentAccountAttachementDto {

    private Long id;

    private String fileName;

    private DocumentAccount documentAccount;

    private boolean isDeleted;

    public DocumentAccountAttachementDto(Long id, String fileName, DocumentAccount documentAccount) {
        super();
        this.id = id;
        this.fileName = fileName;
        this.documentAccount = documentAccount;
    }

}
