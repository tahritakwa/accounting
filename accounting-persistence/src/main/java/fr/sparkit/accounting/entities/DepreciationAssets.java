package fr.sparkit.accounting.entities;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "T_DEPRECIATION_ASSETS")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = { "isDeleted", "deletedToken" })
public class DepreciationAssets implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "DA_ID")
    private Long id;

    @Column(name = "DA_ID_ASSETS")
    private Long idAssets;

    @ManyToOne()
    @JoinColumn(name = "DA_IMMOBILIZATION_ACCOUNT_ID")
    private Account immobilizationAccount;

    @ManyToOne()
    @JoinColumn(name = "DA_AMORTIZATION_ACCOUNT_ID")
    private Account amortizationAccount;

    @Column(name = "DA_CESSION", columnDefinition = "bit default 0")
    private Boolean cession;

    @Column(name = "DA_DATE_CESSION")
    @JsonFormat(pattern = "yyyy/MM/dd HH:mm:ss")
    private LocalDateTime dateCession;

    @Column(name = "DA_AMOUNT_CESSION")
    private BigDecimal amountCession;

    @Column(name = "DA_IS_DELETED", columnDefinition = "bit default 0")
    private boolean isDeleted;

    @Column(name = "DA_DELETED_TOKEN")
    private UUID deletedToken;

    public DepreciationAssets(Long idAssets, Account immobilizationAccount, Account amortizationAccount) {
        super();
        this.idAssets = idAssets;
        this.immobilizationAccount = immobilizationAccount;
        this.amortizationAccount = amortizationAccount;
    }

    public DepreciationAssets(Long idAssets, Account immobilizationAccount, Account amortizationAccount,
            Boolean cession, LocalDateTime dateCession, BigDecimal amountCession) {
        super();
        this.idAssets = idAssets;
        this.immobilizationAccount = immobilizationAccount;
        this.amortizationAccount = amortizationAccount;
        this.cession = cession;
        this.dateCession = dateCession;
        this.amountCession = amountCession;
    }

}
