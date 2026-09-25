package com.smart.therapy.flow.subscription.repository;

import com.smart.therapy.flow.subscription.entity.RefundRetryTask;
import com.smart.therapy.flow.subscription.enums.RefundRetryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface RefundRetryTaskRepository extends JpaRepository<RefundRetryTask, Long> {

    @Query("SELECT t FROM RefundRetryTask t WHERE t.status = :status AND t.nextRetryAt <= :now ORDER BY t.nextRetryAt ASC")
    List<RefundRetryTask> findReadyForRetry(@Param("status") RefundRetryStatus status, @Param("now") Instant now);
}
