package com.barterlink.inactivity.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.inactivity.scheduler",
        name = "enabled",
        havingValue = "true"
)
public class UserInactivityScheduler {

    private final UserInactivityWarningCycleService cycleService;
    private final UserInactivityWarningDeliveryService deliveryService;

    @Scheduled(
            fixedDelayString =
                    "${app.inactivity.scheduler.fixed-delay-ms:60000}"
    )
    public void processInactivityWarnings() {
        try {
            int createdWarnings = cycleService.prepareDueWarnings();

            deliveryService.processPendingWarnings();

            if (createdWarnings > 0) {
                log.info(
                        "Prepared {} inactivity warning records.",
                        createdWarnings
                );
            }
        } catch (RuntimeException exception) {
            log.error(
                    "The inactivity warning cycle could not be completed.",
                    exception
            );
        }
    }
}