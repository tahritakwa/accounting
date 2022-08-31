package fr.sparkit.accounting.entities;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import fr.sparkit.accounting.enumuration.ReportType;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "T_STANDARD_REPORT_LINE", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "SRL_LINE_INDEX", "SRL_REPORT_TYPE", "SRL_DELETED_TOKEN" }) })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = { "isDeleted", "deletedToken" })
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class StandardReportLine implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "SRL_ID")
    private Long id;

    @Column(name = "SRL_LABEL")
    private String label;

    @Column(name = "SRL_FORMULA")
    private String formula;

    @Column(name = "SRL_REPORT_TYPE")
    @Enumerated(EnumType.STRING)
    private ReportType reportType;

    @Column(name = "SRL_LINE_INDEX")
    private String lineIndex;

    @Column(name = "SRL_ANNEX_CODE")
    private String annexCode;

    @Column(name = "SRL_IS_NEGATIVE")
    private boolean isNegative;

    @Column(name = "SRL_IS_TOTAL")
    private boolean isTotal;

    @Column(name = "SRL_IS_DELETED", columnDefinition = "bit default 0")
    private boolean isDeleted;

    @Column(name = "SRL_DELETED_TOKEN")
    private UUID deletedToken;
}
