package fr.sparkit.accounting.entities.account.relations;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import fr.sparkit.accounting.entities.Account;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Table(name = "T_GENERIC_ACCOUNT_RELATION")
public class GenericAccountRelation {
    @Id
    @Column(name = "GAR_ID")
    @GeneratedValue(strategy = GenerationType.TABLE)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne
    @JoinColumn(name = "GAR_ACCOUNT_ID")
    private Account account;

    @Column(name = "GAR_RELATION_ENTITY_ID")
    private Long relationEntityId;

    @Column(name = "GAR_IS_DELETED", columnDefinition = "bit default 0")
    private boolean isDeleted;

    @Column(name = "GAR_DELETED_TOKEN")
    private UUID deletedToken;

    public GenericAccountRelation(Account account, Long relationEntityId, boolean isDeleted, UUID deletedToken) {
        this.account = account;
        this.relationEntityId = relationEntityId;
        this.isDeleted = isDeleted;
        this.deletedToken = deletedToken;
    }
}
