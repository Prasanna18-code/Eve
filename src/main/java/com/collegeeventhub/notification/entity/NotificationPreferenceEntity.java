package com.collegeeventhub.notification.entity;

import com.collegeeventhub.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "notification_preferences",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_notification_preferences_user",
                        columnNames = "user_id"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_notification_preferences_user")
    )
    private UserEntity user;

    @Builder.Default
    @Column(nullable = false)
    private boolean registrationSuccessful = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean eventUpdated = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean eventCancelled = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean eventReminder = true;
}
