package fr.sparkit.accounting.entities;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
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
import fr.sparkit.accounting.constraint.validator.NotAuditedField;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "T_DOCUMENT_ACCOUNT_LINE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AuditedEntity
@ToString(exclude = { "isDeleted", "deletedToken" })
public class DocumentAccountLine implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "DAL_ID")
    private Long id;

    @Convert(converter = LocalDateTimeAttributeConverter.class)
    @Column(name = "DAL_DOCUMENT_LINE_DATE")
    private LocalDateTime documentLineDate;

    @Column(name = "DAL_LABEL")
    private String label;

    @Column(name = "DAL_REFERENCE")
    private String reference;

    @Column(name = "DAL_DEBIT_AMOUNT")
    private BigDecimal debitAmount;

    @Column(name = "DAL_CREDIT_AMOUNT")
    private BigDecimal creditAmount;

    @Column(name = "DAL_LETTER")
    private String letter;

    @ManyToOne
    @JoinColumn(name = "DAL_DOCUMENT_ACCOUNT_ID")
    private DocumentAccount documentAccount;

    @Column(name = "DAL_IS_CLOSE", columnDefinition = "bit default 0")
    private boolean isClose;

    @Column(name = "DAL_RECONCILIATION_DATE")
    private LocalDate reconciliationDate;

    @ManyToOne
    @JoinColumn(name = "DAL_ACCOUNT_ID")
    private Account account;

    @Column(name = "DAL_IS_DELETED", columnDefinition = "bit default 0")
    private boolean isDeleted;

    @NotAuditedField
    @Column(name = "DAL_DELETED_TOKEN")
    private UUID deletedToken;

    public DocumentAccountLine(LocalDateTime documentLineDate, String label, String reference, BigDecimal debitAmount,
            BigDecimal creditAmount, boolean isClose, String letter, LocalDate reconciliationDate) {
        this.documentLineDate = documentLineDate;
        this.label = label;
        this.reference = reference;
        this.debitAmount = debitAmount;
        this.creditAmount = creditAmount;
        this.isDeleted = false;
        this.deletedToken = null;
        this.isClose = isClose;
        this.letter = letter;
        this.reconciliationDate = reconciliationDate;
    }

    public DocumentAccountLine(LocalDateTime documentLineDate, String label, String reference, BigDecimal debitAmount,
            BigDecimal creditAmount, Account account) {
        this.documentLineDate = documentLineDate;
        this.label = label;
        this.reference = reference;
        this.debitAmount = debitAmount;
        this.creditAmount = creditAmount;
        this.account = account;
        this.isDeleted = false;
        this.deletedToken = null;
    }

    public DocumentAccountLine(LocalDateTime documentLineDate, String label, String reference, BigDecimal debitAmount,
            BigDecimal creditAmount, String letter, DocumentAccount documentAccount, boolean isDeleted,
            UUID deletedToken, Account account) {
        super();
        this.documentLineDate = documentLineDate;
        this.label = label;
        this.reference = reference;
        this.debitAmount = debitAmount;
        this.creditAmount = creditAmount;
        this.letter = letter;
        this.documentAccount = documentAccount;
        this.isDeleted = isDeleted;
        this.deletedToken = deletedToken;
        this.account = account;
    }

}
