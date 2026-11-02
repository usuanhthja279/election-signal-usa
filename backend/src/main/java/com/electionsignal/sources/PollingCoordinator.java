package com.electionsignal.sources;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

/** Bounded, non-overlapping provider scheduler; concrete adapters must set HTTP timeouts. */
public final class PollingCoordinator implements AutoCloseable {
    @FunctionalInterface public interface ProviderPoll { void poll(ElectionResultProvider provider) throws Exception; }
    private final Collection<ElectionResultProvider> providers;
    private final Predicate<String> authorized;
    private final ProviderPoll poll;
    private final Duration interval;
    private final Duration baseBackoff;
    private final Duration maxBackoff;
    private final int maxConcurrency;
    private final ScheduledExecutorService timer;
    private final java.util.concurrent.ExecutorService workers;
    private final Semaphore concurrency;
    private final Map<String,AtomicBoolean> inFlight=new ConcurrentHashMap<>();
    private final Map<String,Instant> nextAttempt=new ConcurrentHashMap<>();
    private final Map<String,Instant> nextAllowed=new ConcurrentHashMap<>();
    private final Map<String,Duration> minimumCadence;
    private final Map<String,Integer> failures=new ConcurrentHashMap<>();
    private final RandomGenerator jitter;
    private final AtomicBoolean started=new AtomicBoolean();

    public PollingCoordinator(Collection<ElectionResultProvider> providers,Predicate<String> authorized,
            ProviderPoll poll,Duration interval,int maxConcurrency,
            Duration baseBackoff,Duration maxBackoff,RandomGenerator jitter) {
        this(providers,authorized,poll,interval,maxConcurrency,baseBackoff,maxBackoff,jitter,Map.of());
    }
    public PollingCoordinator(Collection<ElectionResultProvider> providers,Predicate<String> authorized,
            ProviderPoll poll,Duration interval,int maxConcurrency,
            Duration baseBackoff,Duration maxBackoff,RandomGenerator jitter,Map<String,Duration> minimumCadence) {
        if(interval.isNegative()||interval.isZero()||maxConcurrency<1||baseBackoff.isNegative()||maxBackoff.compareTo(baseBackoff)<0) throw new IllegalArgumentException("Invalid collector scheduling limits");
        if(minimumCadence.values().stream().anyMatch(d->d==null||d.isNegative()||d.isZero()))throw new IllegalArgumentException("Invalid provider cadence");
        this.minimumCadence=Map.copyOf(minimumCadence);
        this.providers=List.copyOf(providers);this.authorized=authorized;this.poll=poll;this.interval=interval;
        this.maxConcurrency=maxConcurrency;this.baseBackoff=baseBackoff;this.maxBackoff=maxBackoff;this.jitter=jitter;
        ThreadFactory factory=r->{Thread thread=new Thread(r,"election-source-collector");thread.setDaemon(true);return thread;};
        this.timer=Executors.newSingleThreadScheduledExecutor(factory);this.workers=Executors.newFixedThreadPool(maxConcurrency,factory);this.concurrency=new Semaphore(maxConcurrency);
    }

    public void start(){if(started.compareAndSet(false,true))timer.scheduleWithFixedDelay(this::scheduleCycle,0,interval.toMillis(),java.util.concurrent.TimeUnit.MILLISECONDS);}

    public void scheduleCycle(){
        for(ElectionResultProvider provider:providers){
            String id=provider.providerId();
            Instant now=Instant.now();
            if(!authorized.test(id)||!provider.health().enabled()||now.isBefore(nextAttempt.getOrDefault(id,Instant.MIN))||now.isBefore(nextAllowed.getOrDefault(id,Instant.MIN)))continue;
            AtomicBoolean busy=inFlight.computeIfAbsent(id,ignored->new AtomicBoolean());
            if(!busy.compareAndSet(false,true))continue;
            Duration cadence=minimumCadence.get(id);if(cadence!=null)nextAllowed.put(id,now.plus(cadence));
            workers.submit(()->run(provider,busy));
        }
    }

    private void run(ElectionResultProvider provider,AtomicBoolean busy){
        boolean slot=false;
        try{
            concurrency.acquire();slot=true;
            poll.poll(provider);failures.remove(provider.providerId());nextAttempt.remove(provider.providerId());
        }catch(InterruptedException interrupted){Thread.currentThread().interrupt();}
        catch(Exception failure){
            String id=provider.providerId();int attempt=failures.merge(id,1,Integer::sum);
            Duration retryAfter=failure instanceof ElectionResultProvider.ProviderException providerFailure?providerFailure.retryAfter():provider.health().retryAfter();
            nextAttempt.put(id,Instant.now().plus(retryDelay(baseBackoff,maxBackoff,attempt,retryAfter,jitter)));
        }finally{if(slot)concurrency.release();busy.set(false);}
    }

    static Duration retryDelay(Duration base,Duration maximum,int attempt,Duration retryAfter,RandomGenerator random){
        if(retryAfter!=null&&!retryAfter.isNegative()&&!retryAfter.isZero())return retryAfter;
        long multiplier=1L<<Math.min(Math.max(attempt-1,0),30);
        long ceiling;
        try{ceiling=Math.multiplyExact(base.toMillis(),multiplier);}catch(ArithmeticException overflow){ceiling=maximum.toMillis();}
        ceiling=Math.min(Math.max(ceiling,1),maximum.toMillis());
        long low=Math.max(1,ceiling/2);long jittered=low+(long)(random.nextDouble()*(ceiling-low+1));
        return Duration.ofMillis(Math.min(jittered,maximum.toMillis()));
    }

    public Map<String,String> status(){var result=new java.util.LinkedHashMap<String,String>();for(var provider:providers){String id=provider.providerId();Instant now=Instant.now();result.put(id,!authorized.test(id)?"RIGHTS_BLOCKED":!provider.health().enabled()?"NOT_CONNECTED":inFlight.getOrDefault(id,new AtomicBoolean()).get()?"POLLING":now.isBefore(nextAttempt.getOrDefault(id,Instant.MIN))?"BACKOFF":now.isBefore(nextAllowed.getOrDefault(id,Instant.MIN))?"COOLDOWN":"READY");}return Map.copyOf(result);}

    @Override public void close(){timer.shutdownNow();workers.shutdownNow();started.set(false);}
}
