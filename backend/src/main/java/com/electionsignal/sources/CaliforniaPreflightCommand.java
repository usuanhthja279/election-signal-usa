package com.electionsignal.sources;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
@Component @Profile("demo") @ConditionalOnProperty(name="election.ca-results-preflight",havingValue="true")
public class CaliforniaPreflightCommand implements ApplicationRunner {
 private final ObjectMapper mapper;
 public CaliforniaPreflightCommand(ObjectMapper mapper){this.mapper=mapper;}
 public void run(ApplicationArguments args)throws Exception{var s=new CaliforniaResultsPreflight(mapper).fetch();System.out.println("California Governor feed preflight: "+s.availability()+"; precincts reporting="+s.precinctPercent()+"%; source time as text="+s.sourceTimeText()+". No votes were imported or displayed.");}
}
