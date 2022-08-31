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
import javax.persistence.UniqueConstraint;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "T_ACCOUNT", uniqueConstraints = { @UniqueConstraint(columnNames = { "AC_DELETED_TOKEN", "AC_CODE" }) })
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = { "isDeleted", "deletedToken" })
public class Account implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "AC_ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "AC_CODE")
    private int code;

    @Column(name = "AC_LABEL")
    private String label;

    @ManyToOne
    @JoinColumn(name = "AC_PLAN_ID")
    private ChartAccounts plan;

    @Column(name = "AC_DEBIT_OPENING")
    private BigDecimal debitOpening;

    @Column(name = "AC_CREDIT_OPENING")
    private BigDecimal creditOpening;

    @Column(name = "AC_IS_LITRABLE")
    private boolean literable;

    @Column(name = "AC_IS_RECONCILABLE")
    private boolean reconcilable;

    @Column(name = "AC_TIERS_ID")
    private Long tiersId;

    @Column(name = "AC_IS_DELETED", columnDefinition = "bit default 0")
    private boolean isDeleted;

    @Column(name = "AC_DELETED_TOKEN")
    private UUID deletedToken;

    public Account(int code, String label, ChartAccounts plan, BigDecimal debitOpening, BigDecimal creditOpening) {
        super();
        this.code = code;
        this.label = label;
        this.plan = plan;
        this.debitOpening = debitOpening;
        this.creditOpening = creditOpening;
        this.isDeleted = false;
        this.deletedToken = null;
    }

}
