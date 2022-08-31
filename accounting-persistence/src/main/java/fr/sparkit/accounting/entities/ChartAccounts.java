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

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "T_CHART_ACCOUNTS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = { "accountParent", "isDeleted", "deletedToken" })
public class ChartAccounts implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "CA_ID")
    private Long id;

    @Column(name = "CA_CODE", unique = true, nullable = false)
    private int code;

    @Size(min = 1)
    @Column(name = "CA_LABEL")
    private String label;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "CA_PARENT_ACCOUNT_ID")
    private ChartAccounts accountParent;

    @Column(name = "CA_TO_BALANCED")
    private Boolean toBalanced;

    @Column(name = "CA_IS_DELETED", columnDefinition = "bit default 0")
    private boolean isDeleted;

    @Column(name = "CA_DELETED_TOKEN")
    private UUID deletedToken;

    public ChartAccounts(Long id, int code, String label, ChartAccounts accountParent) {
        this.id = id;
        this.code = code;
        this.label = label;
        this.accountParent = accountParent;
        this.isDeleted = false;
        this.deletedToken = null;
    }

}
