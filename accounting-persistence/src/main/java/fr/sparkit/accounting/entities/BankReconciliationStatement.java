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
import javax.persistence.OneToOne;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "T_BANK_RECONCILIATION_STATEMENT")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class BankReconciliationStatement implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "BRS_ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @OneToOne
    @JoinColumn(name = "BRS_ACCOUNT_ID")
    private Account account;

    @OneToOne
    @JoinColumn(name = "BRS_FISCAL_YEAR_ID")
    private FiscalYear fiscalYear;

    @Column(name = "BRS_CLOSE_MONTH")
    private int closeMonth;

    @Column(name = "BRS_INITIAL_AMOUNT")
    private BigDecimal initialAmount;

    @Column(name = "BRS_FINAL_AMOUNT")
    private BigDecimal finalAmount;

    @Column(name = "BRS_IS_DELETED", columnDefinition = "bit default 0")
    private boolean isDeleted;

    @Column(name = "BRS_DELETED_TOKEN")
    private UUID deletedToken;

}
