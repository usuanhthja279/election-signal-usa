package com.electionsignal.sources;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Source-independent contract. Implementations must not publish until registry rights are approved. */
public interface ElectionResultProvider {
    String providerId();
    ProviderHealth health();
    List<ExternalElection> fetchElectionRegistry(int year) throws ProviderException;
    List<ExternalRaceResult> fetchResults(String electionId) throws ProviderException;
    Optional<ExternalRaceResult> fetchRace(String electionId, String externalRaceId) throws ProviderException;
    List<ReportingUnit> fetchReportingUnits(String electionId, String externalRaceId) throws ProviderException;

    record ProviderHealth(boolean enabled, boolean reachable, String status, Instant checkedAt,
                          Duration retryAfter, String detail) { }
    record ExternalElection(String id, int year, String name) { }
    record ExternalRaceResult(String electionId, String raceId, String office, String state,
                              String district, String subtype, String status, Instant sourceAsOf,
                              String revision, String rawJson,
                              List<ObservationRecorder.CandidateTotal> candidates) { }
    record ReportingUnit(String id, String type, String stateFips, String countyFips,
                         String district, String reportingKind, Double reportingValue) { }

    final class ProviderException extends Exception {
        private final int statusCode;
        private final Duration retryAfter;
        public ProviderException(String message, int statusCode, Duration retryAfter, Throwable cause) {
            super(message, cause); this.statusCode = statusCode; this.retryAfter = retryAfter;
        }
        public int statusCode() { return statusCode; }
        public Duration retryAfter() { return retryAfter; }
    }
}
