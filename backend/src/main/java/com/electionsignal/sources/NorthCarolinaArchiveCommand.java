package com.electionsignal.sources;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Explicit one-shot collector; a database operator must approve the exact source URL first. */
@Component
@Profile("local")
@ConditionalOnProperty(name="election.nc-results-archive", havingValue="true")
public class NorthCarolinaArchiveCommand implements ApplicationRunner {
    private final RawSourceArchive archive;
    public NorthCarolinaArchiveCommand(RawSourceArchive archive) { this.archive=archive; }
    @Override public void run(ApplicationArguments args) throws Exception {
        archive.requireAuthorized("NC_2026_OFFICIAL",NorthCarolinaResultsPreflight.URL.toString());
        byte[] bytes=new NorthCarolinaResultsPreflight().fetchBytes();
        var summary=new NorthCarolinaResultsParser().parse(bytes, row -> {});
        var recorded=archive.record(new RawSourceArchive.Input("NC_2026_OFFICIAL",NorthCarolinaResultsPreflight.URL.toString(),"application/zip",null,200,bytes,false));
        System.out.printf("North Carolina: %d rows, %d county names, %d nonzero rows; archive %s (%s). No results published.%n",summary.rows(),summary.counties(),summary.nonzeroTotalRows(),recorded.id(),recorded.duplicate()?"unchanged":"new");
    }
}
