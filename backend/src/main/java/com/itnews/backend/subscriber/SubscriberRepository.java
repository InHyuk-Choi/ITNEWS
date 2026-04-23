package com.itnews.backend.subscriber;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriberRepository extends JpaRepository<SubscriberEntity, Long> {
    boolean existsByEmail(String email);
    Optional<SubscriberEntity> findByEmail(String email);
    Optional<SubscriberEntity> findByUnsubscribeToken(String token);
    List<SubscriberEntity> findAllByActiveTrueAndVerifiedTrue();
}
