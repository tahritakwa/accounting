package fr.sparkit.accounting.entities;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import fr.sparkit.accounting.constraint.validator.AuditedEntity;
import fr.sparkit.accounting.constraint.validator.NotAuditedField;
import fr.sparkit.accounting.enumuration.ReportType;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "T_REPORT_LINE", uniqueConstraints = { @UniqueConstraint(columnNames = { "RL_FISCAL_YEAR_ID",
        "RL_LINE_INDEX", "RL_REPORT_TYPE", "RL_DELETED_TOKEN" }) })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AuditedEntity
public class ReportLine implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "RL_ID")
    private Long id;

    @Column(name = "RL_LABEL")
    private String label;

    @Column(name = "RL_FORMULA")
    private String formula;

    @Column(name = "RL_REPORT_TYPE")
    @Enumerated(EnumType.STRING)
    private ReportType reportType;

    @Column(name = "RL_LINE_INDEX")
    private String lineIndex;

    @Column(name = "RL_ANNEX_CODE")
    private String annexCode;

    @ManyToOne
    @JoinColumn(name = "RL_FISCAL_YEAR_ID")
    private FiscalYear fiscalYear;

    @Column(name = "RL_AMOUNT")
    private BigDecimal amount;

    @Column(name = "RL_USER")
    private String user;

    @NotAuditedField
    @Column(name = "RL_LAST_UPDATED")
    private LocalDateTime lastUpdated;

    @Column(name = "RL_IS_NEGATIVE")
    private boolean isNegative;

    @Column(name = "RL_IS_MANUALLY_CHANGED")
    private boolean isManuallyChanged;

    @Column(name = "RL_IS_TOTAL")
    private boolean isTotal;

    @Column(name = "RL_IS_DELETED", columnDefinition = "bit default 0")
    private boolean isDeleted;

    @NotAuditedField
    @Column(name = "RL_DELETED_TOKEN")
    private UUID deletedToken;

}
