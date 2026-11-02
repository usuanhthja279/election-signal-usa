package com.electionsignal.history;
import java.nio.file.Path;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
@Component @Profile("local") @ConditionalOnProperty(name="election.house-import")
public class HouseImportCommand implements ApplicationRunner {
  private final HouseResultsImporter importer;
  public HouseImportCommand(HouseResultsImporter importer){this.importer=importer;}
  public void run(ApplicationArguments args)throws Exception{var p=args.getOptionValues("election.house-import");if(p==null||p.size()!=1)throw new IllegalArgumentException("A source file is required");System.out.println("Imported "+importer.importFile(Path.of(p.get(0)))+" source-reported House ballot lines.");}
}
