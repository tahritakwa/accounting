package fr.sparkit.accounting.entities;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import fr.sparkit.accounting.auditing.LocalDateTimeAttributeConverter;
import fr.sparkit.accounting.constraint.validator.AuditedEntity;
import fr.sparkit.accounting.enumuration.DocumentAccountStatus;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "T_DOCUMENT_ACCOUNT")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AuditedEntity
@ToString(exclude = { "isDeleted", "deletedToken" })
public class DocumentAccount implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "DA_ID")
    private Long id;

    @Convert(converter = LocalDateTimeAttributeConverter.class)
    @Column(name = "DA_DOCUMENT_DATE")
    private LocalDateTime documentDate;

    @Convert(converter = LocalDateTimeAttributeConverter.class)
    @Column(name = "DA_CREATION_DOCUMENT_DATE")
    private LocalDateTime creationDocumentDate;

    @Column(name = "DA_LABEL")
    private String label;

    @Column(name = "DA_CODE_DOCUMENT")
    private String codeDocument;

    @ManyToOne
    @JoinColumn(name = "DA_JOURNAL_ID")
    private Journal journal;

    @Column(name = "DA_STATUS", columnDefinition = "int default 0")
    private int indexOfStatus = DocumentAccountStatus.MANUALLY_CREATED.getIndex();

    @Column(name = "DA_IS_DELETED", columnDefinition = "bit default 0")
    private boolean isDeleted;
    @Column(name = "DA_DELETED_TOKEN")
    private UUID deletedToken;

    @ManyToOne
    @JoinColumn(name = "DA_FISCAL_YEAR_ID")
    private FiscalYear fiscalYear;

    public DocumentAccount(String codeDocument, LocalDateTime documentDate, String label, Journal journal,
            int indexOfStatus) {
        this.documentDate = documentDate;
        this.codeDocument = codeDocument;
        this.label = label;
        this.journal = journal;
        this.isDeleted = false;
        this.deletedToken = null;
        this.indexOfStatus = indexOfStatus;
    }
}
