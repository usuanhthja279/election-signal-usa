package com.electionsignal.sources;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
@Component @Profile("demo") @ConditionalOnProperty(name="election.nc-results-preflight",havingValue="true")
public class NorthCarolinaPreflightCommand implements ApplicationRunner {
 public void run(ApplicationArguments args)throws Exception{var s=new NorthCarolinaResultsPreflight().fetch();System.out.println("North Carolina preflight: "+s.rows()+" source rows, "+s.counties()+" county names, "+s.nonzeroTotalRows()+" nonzero total rows. Nothing was imported or published.");}
}
