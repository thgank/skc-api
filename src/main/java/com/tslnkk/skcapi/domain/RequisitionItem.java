package com.tslnkk.skcapi.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "requisition_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_requisition_tru_code",
                columnNames = {"requisition_id", "tru_code"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequisitionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer rowNumber;

    @Column(name = "tru_code", nullable = false)
    private String truCode;

    @Column(nullable = false)
    private String truName;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal count;

    @Column(nullable = false, length = 20)
    private String mkei;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(name = "duration_month")
    private LocalDate durationMonth;

    private String comment;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requisition_id", nullable = false)
    private PurchaseRequisition requisition;
}
