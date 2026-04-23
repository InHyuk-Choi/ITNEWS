package com.itnews.backend.subscriber;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "subscribers")
public class SubscriberEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, unique = true, length = 36)
    private String unsubscribeToken;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean verified = false;

    @Column(nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected SubscriberEntity() {}

    public SubscriberEntity(String email) {
        this.email = email;
        this.unsubscribeToken = UUID.randomUUID().toString();
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getUnsubscribeToken() { return unsubscribeToken; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
