package com.skycrew.repository;

import com.skycrew.model.Notification;
import com.skycrew.model.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByStatus(NotificationStatus status);

    Page<Notification> findByRecipientEmailOrderBySentAtDesc(String email, Pageable pageable);

    List<Notification> findByRelatedRosterId(Long rosterId);
}
