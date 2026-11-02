package com.electionsignal.polling;

import java.time.Duration;
import java.util.List;

/** Explicit disabled adapter: no credentials or network activity are configured. */
public final class DisabledPollingAdapter implements PollingSourceAdapter, PublishedForecastAdapter {
    private final String id;
    public DisabledPollingAdapter(String id){if(id==null||!id.matches("[A-Za-z0-9_-]{1,80}"))throw new IllegalArgumentException("Invalid provider ID");this.id=id;}
    @Override public String providerId(){return id;}
    @Override public boolean enabled(){return false;}
    @Override public Duration requestTimeout(){return Duration.ofSeconds(10);}
    @Override public Health fetchSourceStatus(){return new Health("NOT_CONNECTED","Adapter disabled pending schema and rights verification");}
    @Override public Batch fetchPolls(Filters filters){throw new IllegalStateException("Provider is disabled");}
    @Override public Batch fetchPublishedAverages(Filters filters){throw new IllegalStateException("Provider is disabled");}
    @Override public Batch fetchPublishedForecasts(Filters filters){throw new IllegalStateException("Provider is disabled");}
}
