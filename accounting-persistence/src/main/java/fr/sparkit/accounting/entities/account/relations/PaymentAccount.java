package fr.sparkit.accounting.entities.account.relations;

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

import fr.sparkit.accounting.entities.Account;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "TJ_PAYMENT_ACCOUNT")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class PaymentAccount implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "PA_ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne
    @JoinColumn(name = "PA_TAX_SALES_ACCOUNT")
    private Account taxSalesAccount;

    @ManyToOne
    @JoinColumn(name = "PA_HTAX_SALES_ACCOUNT")
    private Account hTaxSalesAccount;

    @ManyToOne
    @JoinColumn(name = "PA_TAX_PURCHASES_ACCOUNT")
    private Account taxPurchasesAccount;

    @ManyToOne
    @JoinColumn(name = "PA_HTAX_PURCHASES_ACCOUNT")
    private Account hTaxPurchasesAccount;

    @Column(name = "PA_TAX_ID")
    private Long taxId;

    @Column(name = "PA_IS_DELETED", columnDefinition = "bit default 0")
    private boolean isDeleted;

    @Column(name = "PA_DELETED_TOKEN")
    private UUID deletedToken;

}
