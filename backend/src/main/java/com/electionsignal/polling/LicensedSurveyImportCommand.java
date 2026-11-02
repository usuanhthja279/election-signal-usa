package com.electionsignal.polling;
import java.nio.file.Path;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
@Component @Profile("local") @ConditionalOnProperty(name="election.survey-import")
public class LicensedSurveyImportCommand implements ApplicationRunner {
    private final LicensedSurveyImporter importer;
    public LicensedSurveyImportCommand(LicensedSurveyImporter importer){this.importer=importer;}
    @Override public void run(ApplicationArguments args)throws Exception{
        var paths=args.getOptionValues("election.survey-import");if(paths==null||paths.size()!=1)throw new IllegalArgumentException("One input file required");
        System.out.println("Survey import: "+importer.publish(Path.of(paths.get(0))));
    }
}
