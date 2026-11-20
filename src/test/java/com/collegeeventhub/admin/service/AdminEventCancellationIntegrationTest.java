package com.collegeeventhub.admin.service;

import com.collegeeventhub.admin.audit.AdminAuditAction;
import com.collegeeventhub.admin.audit.AdminAuditTargetType;
import com.collegeeventhub.admin.audit.entity.AdminAuditLogEntity;
import com.collegeeventhub.admin.audit.repository.AdminAuditLogRepository;
import com.collegeeventhub.common.enums.EventStatus;
import com.collegeeventhub.common.enums.EventType;
import com.collegeeventhub.common.enums.NotificationType;
import com.collegeeventhub.common.exception.ResourceNotFoundException;
import com.collegeeventhub.event.entity.EventEntity;
import com.collegeeventhub.event.repository.EventRepository;
import com.collegeeventhub.event.service.EventService;
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
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@Rollback
class AdminEventCancellationIntegrationTest {

    @Autowired
    private AdminEventService adminEventService;

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
    private AdminAuditLogRepository adminAuditLogRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void adminCancellationUpdatesStatusAndNotifiesEveryRegistrant() {
        Fixture fixture = createFixture(EventStatus.UPCOMING);

        adminEventService.cancelEvent(fixture.event().getId(), 999L);

        entityManager.flush();
        entityManager.clear();

        assertEquals(
                EventStatus.CANCELLED,
                eventRepository.findById(fixture.event().getId())
                        .orElseThrow()
                        .getStatus()
        );
        assertOneCancellationNotification(
                fixture.firstStudent().getId(),
                fixture.event().getId()
        );
        assertOneCancellationNotification(
                fixture.secondStudent().getId(),
                fixture.event().getId()
        );
        assertAuditLog(
                fixture.event().getId(),
                List.of(EventStatus.UPCOMING)
        );
    }

    @Test
    void repeatedAdminCancellationDoesNotDuplicateNotifications() {
        Fixture fixture = createFixture(EventStatus.UPCOMING);

        adminEventService.cancelEvent(fixture.event().getId(), 999L);
        adminEventService.cancelEvent(fixture.event().getId(), 999L);

        assertAuditLog(
                fixture.event().getId(),
                List.of(EventStatus.UPCOMING, EventStatus.CANCELLED)
        );
        assertOneCancellationNotification(
                fixture.firstStudent().getId(),
                fixture.event().getId()
        );
        assertOneCancellationNotification(
                fixture.secondStudent().getId(),
                fixture.event().getId()
        );
    }

    @Test
    void alreadyCancelledEventRemainsCancelledAndDoesNotDuplicateNotifications() {
        Fixture fixture = createFixture(EventStatus.CANCELLED);

        adminEventService.cancelEvent(fixture.event().getId(), 999L);
        adminEventService.cancelEvent(fixture.event().getId(), 999L);

        assertAuditLog(
                fixture.event().getId(),
                List.of(EventStatus.CANCELLED, EventStatus.CANCELLED)
        );
        assertEquals(
                EventStatus.CANCELLED,
                eventRepository.findById(fixture.event().getId())
                        .orElseThrow()
                        .getStatus()
        );
        assertOneCancellationNotification(
                fixture.firstStudent().getId(),
                fixture.event().getId()
        );
        assertOneCancellationNotification(
                fixture.secondStudent().getId(),
                fixture.event().getId()
        );
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void concurrentAdminAndRegularCancellationShareTheEventLock() throws Exception {
        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW
        );
        Fixture fixture = transactionTemplate.execute(
                status -> createFixture(EventStatus.UPCOMING)
        );
        if (fixture == null) {
            throw new IllegalStateException("Cancellation fixture was not created");
        }

        CountDownLatch firstCancellationFinished = new CountDownLatch(1);
        CountDownLatch releaseFirstTransaction = new CountDownLatch(1);
        CountDownLatch regularCancellationStarted = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<?> adminCancellation = null;
        Future<?> regularCancellation = null;

        try {
            adminCancellation = executor.submit(() ->
                    transactionTemplate.executeWithoutResult(status -> {
                        adminEventService.cancelEvent(fixture.event().getId(), 999L);
                        firstCancellationFinished.countDown();
                        await(releaseFirstTransaction);
                    }));

            assertTrue(
                    firstCancellationFinished.await(10, TimeUnit.SECONDS),
                    "Admin cancellation should finish its work while holding the transaction open"
            );

            regularCancellation = executor.submit(() -> {
                regularCancellationStarted.countDown();
                eventService.cancelEvent(
                        fixture.event().getId(),
                        fixture.leader().getUsername()
                );
            });

            assertTrue(
                    regularCancellationStarted.await(10, TimeUnit.SECONDS),
                    "Regular cancellation should start in a separate worker thread"
            );
            Thread.sleep(250);
            assertFalse(
                    regularCancellation.isDone(),
                    "Regular cancellation should wait for the admin transaction's event lock"
            );

            releaseFirstTransaction.countDown();
            adminCancellation.get(10, TimeUnit.SECONDS);
            regularCancellation.get(10, TimeUnit.SECONDS);

            entityManager.clear();
            assertOneCancellationNotification(
                    fixture.firstStudent().getId(),
                    fixture.event().getId()
            );
            assertOneCancellationNotification(
                    fixture.secondStudent().getId(),
                    fixture.event().getId()
            );
        } finally {
            releaseFirstTransaction.countDown();
            if (adminCancellation != null) {
                adminCancellation.get(10, TimeUnit.SECONDS);
            }
            if (regularCancellation != null) {
                regularCancellation.get(10, TimeUnit.SECONDS);
            }
            executor.shutdownNow();
            transactionTemplate.executeWithoutResult(status -> {
                entityManager.createQuery(
                                "delete from NotificationEntity notification "
                                        + "where notification.event.id = :eventId")
                        .setParameter("eventId", fixture.event().getId())
                        .executeUpdate();
                entityManager.createQuery(
                                "delete from RegistrationEntity registration "
                                        + "where registration.event.id = :eventId")
                        .setParameter("eventId", fixture.event().getId())
                        .executeUpdate();
                entityManager.createQuery(
                                "delete from AdminAuditLogEntity audit "
                                        + "where audit.adminUserId = :adminUserId "
                                        + "and audit.action = :action "
                                        + "and audit.targetType = :targetType "
                                        + "and audit.targetId = :eventId")
                        .setParameter("adminUserId", 999L)
                        .setParameter("action", AdminAuditAction.EVENT_CANCELLED)
                        .setParameter("targetType", AdminAuditTargetType.EVENT)
                        .setParameter("eventId", fixture.event().getId())
                        .executeUpdate();
                eventRepository.deleteById(fixture.event().getId());
                organizationRepository.deleteById(
                        fixture.event().getOrganization().getId()
                );
                userRepository.deleteAllById(List.of(
                        fixture.leader().getId(),
                        fixture.firstStudent().getId(),
                        fixture.secondStudent().getId()
                ));
            });
        }
    }

    @Test
    void invalidEventIdReturnsNotFound() {
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> adminEventService.cancelEvent(Long.MAX_VALUE, 999L)
        );

        assertEquals(
                "Event not found with id: " + Long.MAX_VALUE,
                exception.getMessage()
        );
    }

    private Fixture createFixture(EventStatus status) {
        String suffix = UUID.randomUUID().toString();
        UserEntity leader = saveUser("leader", suffix);
        UserEntity firstStudent = saveUser("student-one", suffix);
        UserEntity secondStudent = saveUser("student-two", suffix);

        OrganizationEntity organization = organizationRepository.save(
                OrganizationEntity.builder()
                        .name("Admin Cancellation Test " + suffix)
                        .leader(leader)
                        .build()
        );

        EventEntity event = eventRepository.save(
                EventEntity.builder()
                        .title("Admin Cancellation Test Event " + suffix)
                        .eventDate(LocalDate.now().plusDays(2))
                        .startTime(LocalTime.of(10, 0))
                        .endTime(LocalTime.of(11, 0))
                        .venue("Test venue")
                        .capacity(10)
                        .registrationDeadline(LocalDateTime.now().plusDays(1))
                        .type(EventType.OFFLINE)
                        .status(status)
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

        return new Fixture(event, leader, firstStudent, secondStudent);
    }

    private UserEntity saveUser(String name, String suffix) {
        return userRepository.save(
                UserEntity.builder()
                        .username(name.replace('-', '_') + "_" + suffix)
                        .email(name.replace('-', '.') + "_" + suffix + "@example.com")
                        .password("test-password")
                        .firstName("Test")
                        .lastName(name)
                        .build()
        );
    }

    private void assertOneCancellationNotification(Long userId, Long eventId) {
        List<NotificationEntity> cancellationNotifications =
                notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                        .stream()
                        .filter(notification ->
                                notification.getType() == NotificationType.EVENT_CANCELLED
                                        && notification.getEvent().getId().equals(eventId))
                        .toList();

        assertEquals(1, cancellationNotifications.size());
    }

    private void assertAuditLog(
            Long eventId,
            List<EventStatus> previousStatuses) {
        List<AdminAuditLogEntity> logs = adminAuditLogRepository
                .findByAdminUserIdAndActionAndTargetTypeAndTargetIdOrderByIdAsc(
                        999L,
                        AdminAuditAction.EVENT_CANCELLED,
                        AdminAuditTargetType.EVENT,
                        eventId
                );
        assertEquals(previousStatuses.size(), logs.size());
        for (int i = 0; i < previousStatuses.size(); i++) {
            assertEquals(
                    "status changed from " + previousStatuses.get(i)
                            + " to " + EventStatus.CANCELLED,
                    logs.get(i).getDetails()
            );
        }
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting to release transaction");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to release transaction", exception);
        }
    }

    private record Fixture(
            EventEntity event,
            UserEntity leader,
            UserEntity firstStudent,
            UserEntity secondStudent) {
    }
}
