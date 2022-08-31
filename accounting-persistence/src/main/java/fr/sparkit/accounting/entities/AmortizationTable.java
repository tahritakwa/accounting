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

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "T_AMORTIZATION_TABLE")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AmortizationTable implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "AT_ID")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "AT_ASSETS_ID")
    private DepreciationAssets assets;

    @ManyToOne
    @JoinColumn(name = "AT_YEAR_ID")
    private FiscalYear fiscalYear;

    @Column(name = "AT_ACQUISITION_VALUE", precision = 19, scale = 3)
    private BigDecimal acquisitionValue;

    @Column(name = "AT_PREVIOUS_DEPRECIATION", precision = 19, scale = 3)
    private BigDecimal previousDepreciation;

    @Column(name = "AT_ANNUITY_EXERCISE", precision = 19, scale = 3)
    private BigDecimal annuityExercise;

    @Column(name = "AT_VCN", precision = 19, scale = 3)
    private BigDecimal vcn;

    @Column(name = "AT_IS_DELETED", columnDefinition = "bit default 0")
    private boolean isDeleted;

    @Column(name = "AT_DELETED_TOKEN")
    private UUID deletedToken;

    public AmortizationTable(DepreciationAssets depreciationAssets, FiscalYear fiscalYear, BigDecimal acquisitionValue,
            BigDecimal previousDepreciation, BigDecimal annuityExercise, BigDecimal vcn) {
        this.assets = depreciationAssets;
        this.fiscalYear = fiscalYear;
        this.acquisitionValue = acquisitionValue;
        this.previousDepreciation = previousDepreciation;
        this.annuityExercise = annuityExercise;
        this.vcn = vcn;
        this.isDeleted = false;
        this.deletedToken = null;
    }
}
