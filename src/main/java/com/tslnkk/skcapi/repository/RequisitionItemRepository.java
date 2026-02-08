package com.tslnkk.skcapi.repository;

import com.tslnkk.skcapi.domain.RequisitionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RequisitionItemRepository extends JpaRepository<RequisitionItem, Long> {

    Optional<RequisitionItem> findByIdAndRequisitionId(Long id, Long requisitionId);
}
