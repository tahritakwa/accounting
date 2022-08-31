package fr.sparkit.accounting.entities;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import fr.sparkit.accounting.auditing.MoneySerializer;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "T_TEMPLATE_ACCOUNTING_DETAILS")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = { "isDeleted", "deletedToken" })
public class TemplateAccountingDetails implements Serializable {

    /**
    *
    */
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "TAD_ID")
    private Long id;

    @Column(name = "TAD_LABEL")
    private String label;

    @ManyToOne
    @JoinColumn(name = "TAD_TEMPLATE_ACCOUNTING_ID")
    private TemplateAccounting templateAccounting;

    @ManyToOne
    @JoinColumn(name = "TAD_ACCOUNT_ID")
    private Account account;

    @JsonSerialize(using = MoneySerializer.class)
    @Column(name = "TAD_DEBIT_AMOUNT")
    private BigDecimal debitAmount;

    @JsonSerialize(using = MoneySerializer.class)
    @Column(name = "TAD_CREDIT_AMOUNT")
    private BigDecimal creditAmount;

    @Column(name = "TAD_IS_DELETED", columnDefinition = "bit default 0")
    private boolean isDeleted;

    @Column(name = "TAD_DELETED_TOKEN")
    private UUID deletedToken;

    public TemplateAccountingDetails(Long id, String label, BigDecimal debitAmount, BigDecimal creditAmount,
            boolean isDeleted, UUID deletedToken) {
        super();
        this.id = id;
        this.debitAmount = debitAmount;
        this.creditAmount = creditAmount;
        this.label = label;
        this.isDeleted = isDeleted;
        this.deletedToken = deletedToken;
    }

}
