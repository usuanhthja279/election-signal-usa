package com.electionsignal.sources;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class PollingCoordinatorTests {
    @Test void backoffIsBoundedAndHonorsRetryAfter() {
        var random=new java.util.Random(1);
        assertTrue(PollingCoordinator.retryDelay(Duration.ofSeconds(2),Duration.ofSeconds(30),1,null,random).toMillis()<=2000);
        var capped=PollingCoordinator.retryDelay(Duration.ofSeconds(2),Duration.ofSeconds(30),9,null,random);
        assertTrue(capped.compareTo(Duration.ofSeconds(15))>=0 && capped.compareTo(Duration.ofSeconds(30))<=0);
        assertEquals(Duration.ofSeconds(4),PollingCoordinator.retryDelay(Duration.ofSeconds(2),Duration.ofSeconds(30),4,Duration.ofSeconds(4),random));
        assertEquals(Duration.ofMinutes(1),PollingCoordinator.retryDelay(Duration.ofSeconds(2),Duration.ofSeconds(30),4,Duration.ofMinutes(1),random));
    }

    @Test void schedulerSkipsUnauthorizedProvidersAndDoesNotOverlap() throws Exception {
        var provider=new FakeProvider();var polls=new CountDownLatch(1);var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        try(var scheduler=new PollingCoordinator(List.of(provider),id->false,p->{entered.countDown();release.await();polls.countDown();},Duration.ofSeconds(1),1,Duration.ofMillis(10),Duration.ofSeconds(1),new java.util.Random(1))) {
            scheduler.scheduleCycle();assertFalse(entered.await(50,TimeUnit.MILLISECONDS));assertEquals("RIGHTS_BLOCKED",scheduler.status().get("fixture"));
        }
        try(var scheduler=new PollingCoordinator(List.of(provider),id->true,p->{entered.countDown();release.await();},Duration.ofSeconds(1),1,Duration.ofMillis(10),Duration.ofSeconds(1),new java.util.Random(1))) {
            scheduler.scheduleCycle();assertTrue(entered.await(2,TimeUnit.SECONDS));scheduler.scheduleCycle();assertTrue(scheduler.status().get("fixture").equals("POLLING"));release.countDown();
        }
    }

    @Test void providerCadencePreventsEarlyRepeat() throws Exception {
        var provider=new FakeProvider();var called=new CountDownLatch(1);var count=new java.util.concurrent.atomic.AtomicInteger();
        try(var scheduler=new PollingCoordinator(List.of(provider),id->true,p->{count.incrementAndGet();called.countDown();},Duration.ofSeconds(1),1,Duration.ofMillis(10),Duration.ofSeconds(1),new java.util.Random(1),java.util.Map.of("fixture",Duration.ofMinutes(2)))){
            scheduler.scheduleCycle();assertTrue(called.await(2,TimeUnit.SECONDS));
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(2);while(scheduler.status().get("fixture").equals("POLLING")&&System.nanoTime()<deadline)Thread.onSpinWait();
            assertEquals("COOLDOWN",scheduler.status().get("fixture"));scheduler.scheduleCycle();assertEquals(1,count.get());
        }
    }

    private static final class FakeProvider implements ElectionResultProvider {
        public String providerId(){return "fixture";}
        public ProviderHealth health(){return new ProviderHealth(true,true,"READY",Instant.now(),null,"Test provider");}
        public List<ExternalElection> fetchElectionRegistry(int year){return List.of();}
        public List<ExternalRaceResult> fetchResults(String electionId){return List.of();}
        public Optional<ExternalRaceResult> fetchRace(String electionId,String raceId){return Optional.empty();}
        public List<ReportingUnit> fetchReportingUnits(String electionId,String raceId){return List.of();}
    }
}
