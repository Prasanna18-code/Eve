package com.collegeeventhub.notification.scheduler;

import com.collegeeventhub.common.enums.NotificationType;
import com.collegeeventhub.common.enums.EventStatus;
import com.collegeeventhub.event.entity.EventEntity;
import com.collegeeventhub.event.repository.EventRepository;
import com.collegeeventhub.notification.repository.NotificationRepository;
import com.collegeeventhub.notification.service.NotificationService;
import com.collegeeventhub.registration.entity.RegistrationEntity;
import com.collegeeventhub.registration.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 9 * * *")
    public void sendEventReminders() {

        LocalDate tomorrow = LocalDate.now().plusDays(1);

        List<EventEntity> events =
                eventRepository.findByEventDateAndStatusNotIn(
                        tomorrow,
                        List.of(EventStatus.CANCELLED, EventStatus.DELETED)
                );

        for (EventEntity event : events) {
            List<RegistrationEntity> registrations =
                    registrationRepository.findByEventId(event.getId());

            for (RegistrationEntity registration : registrations) {
                Long userId = registration.getUser().getId();
                boolean alreadySent =
                        notificationRepository.existsByUserIdAndEventIdAndType(
                                userId,
                                event.getId(),
                                NotificationType.EVENT_REMINDER
                        );

                if (!alreadySent) {
                    notificationService.createNotification(
                            userId,
                            event.getId(),
                            "Event Reminder",
                            "Reminder: \"" + event.getTitle()
                                    + "\" is scheduled for tomorrow at "
                                    + event.getStartTime(),
                            NotificationType.EVENT_REMINDER
                    );
                }
            }
        }
    }
}