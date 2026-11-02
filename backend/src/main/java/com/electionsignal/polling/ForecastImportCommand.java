package com.electionsignal.polling;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Only an explicit local CLI invocation may initiate a provider request. */
@Component @Profile("local") @ConditionalOnProperty(name="election.forecast-import")
public class ForecastImportCommand implements ApplicationRunner {
    private final PollingForecastImporter importer; private final ObjectMapper mapper;
    public ForecastImportCommand(PollingForecastImporter importer,ObjectMapper mapper){this.importer=importer;this.mapper=mapper;}
    @Override public void run(ApplicationArguments arguments)throws Exception{
        var values=arguments.getOptionValues("election.forecast-import");
        if(values==null||values.size()!=1)throw new IllegalArgumentException("One dataset is required");
        var response=new PollingForecastClient(mapper).fetch(values.get(0));
        int count=importer.importResponse(response);
        System.out.println("Imported "+count+" new attributed publisher forecast snapshots. Poll rows were not imported.");
    }
}
