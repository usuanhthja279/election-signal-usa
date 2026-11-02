package com.electionsignal.polling;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Provider contracts are disabled by default; an adapter is not evidence of usage rights. */
public interface PollingSourceAdapter {
    record Filters(int year, String office, String state, String district, String cursor, int pageSize) { }
    record Batch(List<Map<String,Object>> rows, String nextCursor, String etag) { }
    record Health(String status, String message) { }
    String providerId();
    boolean enabled();
    Duration requestTimeout();
    Batch fetchPolls(Filters filters) throws Exception;
    Batch fetchPublishedAverages(Filters filters) throws Exception;
    Health fetchSourceStatus();
}
