package com.electionsignal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.electionsignal.api.HealthController;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HealthControllerTests {
    @Test
    void healthNeverClaimsElectionFeedsAreConnected() {
        var health = new HealthController().health();
        assertEquals("DEMO", health.get("dataMode"));
        assertEquals(true, health.get("isTestData"));
        var connected = (Map<?, ?>) health.get("connected");
        assertEquals(false, connected.get("live"));
        assertEquals(false, connected.get("forecastModel"));
    }
}
