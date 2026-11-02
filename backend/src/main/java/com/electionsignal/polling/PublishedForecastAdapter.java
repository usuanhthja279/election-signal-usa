package com.electionsignal.polling;

public interface PublishedForecastAdapter {
    String providerId();
    boolean enabled();
    PollingSourceAdapter.Batch fetchPublishedForecasts(PollingSourceAdapter.Filters filters) throws Exception;
}
