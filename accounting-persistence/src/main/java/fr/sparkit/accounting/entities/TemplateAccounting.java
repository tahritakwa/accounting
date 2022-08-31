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
import javax.validation.constraints.Size;

import fr.sparkit.accounting.constants.AccountingConstants;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "T_TEMPLATE_ACCOUNTING")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@ToString(exclude = { "isDeleted", "deletedToken" })
public class TemplateAccounting implements Serializable {

    /**
    *
    */
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "TA_ID")
    private Long id;

    @Column(name = "TA_LABEL")
    @Size(min = AccountingConstants.ENTITY_DEFAULT_LABEL_MIN_LENGTH, max = AccountingConstants.ENTITY_DEFAULT_LABEL_MAX_LENGTH)
    private String label;

    @ManyToOne
    @JoinColumn(name = "TA_JOURNAL_ID")
    private Journal journal;

    @Column(name = "TA_IS_DELETED", columnDefinition = "bit default 0")
    private boolean isDeleted;

    @Column(name = "TA_DELELTED_TOKEN")
    private UUID deletedToken;

    public TemplateAccounting(Long id, String label, Journal journal, boolean isDeleted, UUID deletedToken) {
        super();
        this.id = id;
        this.label = label;
        this.journal = journal;
        this.isDeleted = isDeleted;
        this.deletedToken = deletedToken;
    }

}
