package com.collegeeventhub.event.service;

import com.collegeeventhub.common.enums.EventStatus;
import com.collegeeventhub.common.enums.EventType;
import com.collegeeventhub.common.enums.NotificationType;
import com.collegeeventhub.event.entity.EventEntity;
import com.collegeeventhub.event.repository.EventRepository;
import com.collegeeventhub.notification.entity.NotificationEntity;
import com.collegeeventhub.notification.repository.NotificationRepository;
import com.collegeeventhub.organization.entity.OrganizationEntity;
import com.collegeeventhub.organization.repository.OrganizationRepository;
import com.collegeeventhub.registration.entity.RegistrationEntity;
import com.collegeeventhub.registration.repository.RegistrationRepository;
import com.collegeeventhub.user.entity.UserEntity;
import com.collegeeventhub.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
@Rollback
class EventCancellationNotificationIntegrationTest {

    @Autowired
    private EventService eventService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void cancellationNotifiesEveryRegisteredStudentWithTheCancelledEventLinked() {
        String suffix = UUID.randomUUID().toString();
        UserEntity leader = saveUser("leader", suffix);
        UserEntity firstStudent = saveUser("student-one", suffix);
        UserEntity secondStudent = saveUser("student-two", suffix);

        OrganizationEntity organization = organizationRepository.save(
                OrganizationEntity.builder()
                        .name("Cancellation Test " + suffix)
                        .leader(leader)
                        .build()
        );

        EventEntity event = eventRepository.save(
                EventEntity.builder()
                        .title("Cancellation Test Event " + suffix)
                        .eventDate(LocalDate.now().plusDays(2))
                        .startTime(LocalTime.of(10, 0))
                        .endTime(LocalTime.of(11, 0))
                        .venue("Test venue")
                        .capacity(10)
                        .registrationDeadline(LocalDateTime.now().plusDays(1))
                        .type(EventType.OFFLINE)
                        .organization(organization)
                        .build()
        );

        registrationRepository.saveAll(List.of(
                RegistrationEntity.builder()
                        .user(firstStudent)
                        .event(event)
                        .build(),
                RegistrationEntity.builder()
                        .user(secondStudent)
                        .event(event)
                        .build()
        ));

        eventService.cancelEvent(event.getId(), leader.getUsername());

        entityManager.flush();
        entityManager.clear();

        EventEntity cancelledEvent = eventRepository.findById(event.getId())
                .orElseThrow();
        assertEquals(EventStatus.CANCELLED, cancelledEvent.getStatus());

        assertCancellationNotification(firstStudent.getId(), event.getId());
        assertCancellationNotification(secondStudent.getId(), event.getId());
    }

    private UserEntity saveUser(String role, String suffix) {
        return userRepository.save(
                UserEntity.builder()
                        .username(role.replace('-', '_') + "_" + suffix)
                        .email(role.replace('-', '.') + "_" + suffix + "@example.com")
                        .password("test-password")
                        .firstName("Test")
                        .lastName(role)
                        .build()
        );
    }

    private void assertCancellationNotification(Long userId, Long eventId) {
        List<NotificationEntity> cancellationNotifications =
                notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                        .stream()
                        .filter(notification ->
                                notification.getType() == NotificationType.EVENT_CANCELLED)
                        .toList();

        assertEquals(1, cancellationNotifications.size());
        assertEquals(
                eventId,
                cancellationNotifications.get(0).getEvent().getId()
        );
    }
}
