package com.rnhappysmile.java_labs.module.auth.repository;

import com.rnhappysmile.java_labs.module.auth.domain.EmailOutbox;
import com.rnhappysmile.java_labs.module.auth.domain.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EmailOutboxRepository extends JpaRepository<EmailOutbox, Long> {
    List<EmailOutbox> findByStatusOrderByCreatedAtAsc(OutboxStatus status, Pageable pageable);

    @Modifying
    @Query("UPDATE EmailOutbox e SET e.status = :toStatus, e.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE e.id = :id AND e.status = :fromStatus")
    int updateStatus(@Param("id") Long id, @Param("fromStatus") OutboxStatus fromStatus, @Param("toStatus") OutboxStatus toStatus);
}
