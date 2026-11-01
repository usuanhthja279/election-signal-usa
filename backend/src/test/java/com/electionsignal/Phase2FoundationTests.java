package com.electionsignal;

import static org.junit.jupiter.api.Assertions.*;
import com.electionsignal.history.HistoricalRowValidator;
import com.electionsignal.sources.ObservationFingerprint;
import com.electionsignal.sources.SourceRights;
import java.util.List;
import org.junit.jupiter.api.Test;

class Phase2FoundationTests {
    @Test void providerCannotPublishWithoutApprovedRedistributionRightsAndEnablement() {
        assertFalse(new SourceRights("civicAPI", null, false, false, false).mayPublish());
        assertFalse(new SourceRights("AP", "contract", true, true, false).mayPublish());
        assertTrue(new SourceRights("licensed", "terms-v1", true, true, true).mayPublish());
    }
    @Test void rawPayloadChecksumIsStableAndChangesWithBytes() {
        byte[] payload = "{\"race\":\"fixture\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(ObservationFingerprint.sha256(payload), ObservationFingerprint.sha256(payload.clone()));
        assertNotEquals(ObservationFingerprint.sha256(payload), ObservationFingerprint.sha256("{}".getBytes()));
        assertThrows(IllegalArgumentException.class, () -> ObservationFingerprint.sha256(null));
    }
    @Test void certifiedHouseRowsRequireExactDistrictAndPreserveLeadingZero() {
        var house = row("HOUSE", "DISTRICT", "01", "01", 123L);
        assertEquals(1, HistoricalRowValidator.validate(List.of(house)).size());
        assertEquals("01", house.districtCode());
        assertThrows(IllegalArgumentException.class, () -> HistoricalRowValidator.validate(List.of(row("HOUSE", "COUNTY", "001", "01", 123L))));
    }
    @Test void missingVotesStayNullAndZeroRemainsAnObservedValue() {
        assertDoesNotThrow(() -> HistoricalRowValidator.validate(List.of(row("GOVERNOR", "STATE", "TX", null, null))));
        assertDoesNotThrow(() -> HistoricalRowValidator.validate(List.of(row("GOVERNOR", "STATE", "TX", null, 0L))));
        assertThrows(IllegalArgumentException.class, () -> HistoricalRowValidator.validate(List.of(row("GOVERNOR", "STATE", "TX", null, -1L))));
    }
    @Test void duplicatesAndUncertifiedRowsAreRejected() {
        var row = row("SENATE", "STATE", "TX", null, 0L);
        assertThrows(IllegalArgumentException.class, () -> HistoricalRowValidator.validate(List.of(row, row)));
        var uncertified = new HistoricalRowValidator.Row(row.contestId(),row.office(),row.candidateId(),row.geoType(),row.geoId(),row.stateFips(),row.countyFips(),row.districtCode(),row.geographyVersion(),row.voteMethod(),"UNOFFICIAL",row.votes());
        assertThrows(IllegalArgumentException.class, () -> HistoricalRowValidator.validate(List.of(uncertified)));
    }
    private HistoricalRowValidator.Row row(String office,String geoType,String geoId,String district,Long votes) {
        return new HistoricalRowValidator.Row("contest-2024",office,"candidate-1",geoType,geoId,
                "48",geoType.equals("COUNTY")?"48001":null,district,"CENSUS_2024", "TOTAL", "CERTIFIED", votes);
    }
}
