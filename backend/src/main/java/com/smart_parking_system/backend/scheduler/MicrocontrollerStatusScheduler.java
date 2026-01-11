package com.smart_parking_system.backend.scheduler;

import com.smart_parking_system.backend.entity.Microcontroller;
import com.smart_parking_system.backend.repository.MicrocontrollerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MicrocontrollerStatusScheduler {

    private final MicrocontrollerRepository microcontrollerRepository;

    @Value("${microcontroller.offline-threshold-seconds:10}")
    private int offlineThresholdSeconds;

    @Scheduled(fixedRateString = "${microcontroller.status-check-interval-ms:5000}")
    @Transactional
    public void checkMicrocontrollerStatus() {
        Instant threshold = Instant.now().minusSeconds(offlineThresholdSeconds);

        List<Microcontroller> staleMicrocontrollers = microcontrollerRepository
                .findOnlineWithLastSeenBefore(threshold);

        if (staleMicrocontrollers.isEmpty()) {
            return;
        }

        for (Microcontroller mc : staleMicrocontrollers) {
            mc.setOnline(false);
            microcontrollerRepository.save(mc);

            log.info("Microcontroller {} marked offline (last seen: {})",
                    mc.getMcCode(), mc.getLastSeen());
        }

        log.info("Marked {} microcontroller(s) as offline", staleMicrocontrollers.size());
    }
}
