package com.collegeeventhub.registration.entity;

import com.collegeeventhub.common.enums.RegistrationStatus;
import com.collegeeventhub.event.entity.EventEntity;
import com.collegeeventhub.user.entity.UserEntity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "registrations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_event",
                        columnNames = {"user_id", "event_id"}
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private EventEntity event;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        @Builder.Default
        private RegistrationStatus status = RegistrationStatus.REGISTERED;

    @Column(nullable = false, updatable = false)
    private LocalDateTime registeredAt;

        private LocalDateTime cancelledAt;

    @PrePersist
    protected void onCreate() {
        registeredAt = LocalDateTime.now();
    }
}
