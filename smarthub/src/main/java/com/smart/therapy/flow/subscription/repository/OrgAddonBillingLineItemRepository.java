package com.smart.therapy.flow.subscription.repository;

import com.smart.therapy.flow.subscription.entity.OrgAddonBillingLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrgAddonBillingLineItemRepository extends JpaRepository<OrgAddonBillingLineItem, Long> {
}
