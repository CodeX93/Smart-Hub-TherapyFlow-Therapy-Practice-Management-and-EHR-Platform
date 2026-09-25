package com.smart.therapy.flow.superadmin.repository;

import com.smart.therapy.flow.superadmin.entity.PlatformNotificationTrigger;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlatformNotificationTriggerRepository extends JpaRepository<PlatformNotificationTrigger, Long> {
    default List<PlatformNotificationTrigger> findAllOrdered() {
        return findAll(Sort.by(Sort.Direction.ASC, "eventType", "priority", "name"));
    }
}
