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
import javax.persistence.OneToOne;
import javax.persistence.Table;

import fr.sparkit.accounting.auditing.LocalDateTimeAttributeConverter;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "T_BILL_DOCUMENT")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = { "isDeleted", "deletedToken" })
public class BillDocument implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "BD_ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "BD_BILL_ID", unique = true)
    private Long idBill;

    @OneToOne
    @JoinColumn(name = "BD_DOCUMENT_ACCOUNT_ID")
    private DocumentAccount documentAccount;

    @Convert(converter = LocalDateTimeAttributeConverter.class)
    @Column(name = "BD_CREATION_DATE")
    private LocalDateTime creationDate;

    @Column(name = "BD_DOCUMENT_TYPE")
    private String documentType;

    @Column(name = "BD_IS_DELETED", columnDefinition = "bit default 0")
    private boolean isDeleted;

    @Column(name = "BD_DELETED_TOKEN")
    private UUID deletedToken;

    public BillDocument(Long idBill, DocumentAccount documentAccount, LocalDateTime creationDate, String documentType) {
        this.idBill = idBill;
        this.documentAccount = documentAccount;
        this.creationDate = creationDate;
        this.documentType = documentType;
    }

}
