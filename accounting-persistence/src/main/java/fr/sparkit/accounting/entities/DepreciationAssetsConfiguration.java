package fr.sparkit.accounting.entities;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "T_DEPRECIATION_ASSETS_CONFIGURATION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = { "IsDeleted", "DeletedToken" })
@Builder
public class DepreciationAssetsConfiguration implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "DC_ID")
    private Long id;

    @Column(name = "DC_ID_CATEGORY")
    private long idCategory;

    @Column(name = "DC_DEPRECIATION_PERIOD")
    private int depreciationPeriod;

    @ManyToOne()
    @JoinColumn(name = "DC_IMMOBILIZATION_ACCOUNT_ID")
    private Account immobilizationAccount;

    @ManyToOne()
    @JoinColumn(name = "DC_AMORTIZATION_ACCOUNT_ID")
    private Account amortizationAccount;

    @Column(name = "DC_IMMOBILIZATION_TYPE")
    private String immobilizationType;

    @Column(name = "DC_IS_DELETED", columnDefinition = "bit default 0")
    private boolean isDeleted;

    @Column(name = "DC_DELETED_TOKEN")
    private UUID deletedToken;

    public DepreciationAssetsConfiguration(long idCategory, int depreciationPeriod, String immobilizationType) {
        this.idCategory = idCategory;
        this.depreciationPeriod = depreciationPeriod;
        this.immobilizationType = immobilizationType;
    }
}
