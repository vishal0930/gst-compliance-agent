package com.gstcompliance.repository;

import com.gstcompliance.model.Gstr2bLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface Gstr2bLineItemRepository extends JpaRepository<Gstr2bLineItem, UUID> {

    List<Gstr2bLineItem> findByGstr2bInvoiceId(UUID gstr2bInvoiceId);

    List<Gstr2bLineItem> findByHsnCode(String hsnCode);
}