package fr.sparkit.accounting.entities;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "T_USER_PREFERENCE", uniqueConstraints = { @UniqueConstraint(columnNames = { "UP_USER_EMAIL" }) })
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = { "isDeleted", "deletedToken" })
public class UserPreference implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "UP_ID")
    private Long id;

    @Column(name = "UP_USER_EMAIL")
    private String userEmail;

    @OneToOne()
    @JoinColumn(name = "UP_CURRENT_FISCAL_YEAR_ID")
    private FiscalYear currentFiscalYear;

    @OneToOne()
    @JoinColumn(name = "UP_JOURNAL_ID")
    private Journal journal;

    @Column(name = "UP_IS_DELETED", columnDefinition = "bit default 0")
    private boolean isDeleted;

    @Column(name = "UP_DELETED_TOKEN")
    private UUID deletedToken;

}
