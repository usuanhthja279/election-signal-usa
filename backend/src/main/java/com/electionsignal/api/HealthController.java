package com.electionsignal.api;

import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.annotation.Profile;

/** Initial service health only; this controller does not provide election data. */
@RestController
@Profile("demo")
public class HealthController {
    @GetMapping("/api/v1/health")
    public Map<String, Object> health() {
        return Map.of(
            "service", "election-signal-phase1",
            "status", "ok",
            "phase", 1,
            "dataMode", "DEMO",
            "isTestData", true,
            "connected", Map.of("live", false, "history", false, "polls", false,
                "exitPolls", false, "forecastModel", false),
            "serverTimeUtc", Instant.now().toString());
    }
}
