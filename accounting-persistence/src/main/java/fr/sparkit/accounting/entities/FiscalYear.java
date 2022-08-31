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
import javax.persistence.Table;

import fr.sparkit.accounting.auditing.LocalDateTimeAttributeConverter;
import fr.sparkit.accounting.constraint.validator.AuditedEntity;
import fr.sparkit.accounting.constraint.validator.NotAuditedField;
import fr.sparkit.accounting.enumuration.FiscalYearClosingState;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "T_FISCAL_YEAR")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AuditedEntity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FiscalYear implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FY_ID")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "FY_NAME", unique = true)
    private String name;

    @Convert(converter = LocalDateTimeAttributeConverter.class)
    @Column(name = "FY_START_DATE")
    private LocalDateTime startDate;

    @Convert(converter = LocalDateTimeAttributeConverter.class)
    @Column(name = "FY_END_DATE")
    private LocalDateTime endDate;

    @Convert(converter = LocalDateTimeAttributeConverter.class)
    @Column(name = "FY_CLOSING_DATE")
    private LocalDateTime closingDate;

    @Convert(converter = LocalDateTimeAttributeConverter.class)
    @Column(name = "FY_CONCLUSION_DATE")
    private LocalDateTime conclusionDate;

    @Column(name = "FY_CLOSING_STATE", columnDefinition = "int default 0")
    private int closingState = FiscalYearClosingState.OPEN.getValue();

    @Column(name = "FY_IS_DELETED", columnDefinition = "bit default 0")
    private boolean isDeleted;

    @NotAuditedField
    @Column(name = "FY_DELETED_TOKEN")
    private UUID deletedToken;
}
