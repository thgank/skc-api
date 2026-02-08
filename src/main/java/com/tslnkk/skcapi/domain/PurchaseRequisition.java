package com.tslnkk.skcapi.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_requisitions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseRequisition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String number;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequisitionStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdFrom;

    private LocalDateTime updatedFrom;

    @Column(nullable = false)
    private String organizerId;

    @Column(name = "total_lot_sum_no_nds", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalLotSumNoNds = BigDecimal.ZERO;

    @OneToMany(mappedBy = "requisition", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RequisitionItem> items = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (createdFrom == null) {
            createdFrom = LocalDateTime.now();
        }
        if (updatedFrom == null) {
            updatedFrom = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedFrom = LocalDateTime.now();
    }

    /**
     * Пересчитать totalLotSumNoNds как SUM(price × count) всех позиций.
     */
    public void recalculateTotal() {
        this.totalLotSumNoNds = items.stream()
                .map(item -> item.getPrice().multiply(item.getCount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public boolean isDraft() {
        return this.status == RequisitionStatus.DRAFT;
    }
}
