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
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.Size;

import fr.sparkit.accounting.auditing.LocalDateTimeAttributeConverter;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "T_JOURNAL", uniqueConstraints = { @UniqueConstraint(columnNames = { "JN_DELETED_TOKEN", "JN_CODE", }),
        @UniqueConstraint(columnNames = { "JN_DELETED_TOKEN", "JN_LABEL" }) })
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = { "isDeleted", "deletedToken" })
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Journal implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "JN_ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "JN_CODE", unique = true, nullable = false)
    private String code;

    @Column(name = "JN_LABEL", unique = true, nullable = false)
    private String label;

    @Column(name = "JN_IS_MOVABLE")
    private boolean isMovable;

    @Column(name = "JN_IS_RECONCILABLE")
    private boolean reconcilable;

    @Column(name = "JN_IS_CASH_FLOW")
    private boolean cashFlow;

    @Column(name = "JN_CREATED_DATE")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime createdDate;

    @Column(name = "JN_IS_DELETED", columnDefinition = "bit default 0")
    private boolean isDeleted;

    @Column(name = "JN_DELETED_TOKEN")
    private UUID deletedToken;

    public Journal(Long id, String code, String label, LocalDateTime createdDate, boolean reconcilable,
            boolean cashFlow) {
        this.id = id;
        this.code = code;
        this.label = label;
        this.createdDate = createdDate;
        this.reconcilable = reconcilable;
        this.cashFlow = cashFlow;
        this.isDeleted = false;
        this.deletedToken = null;
    }

}
