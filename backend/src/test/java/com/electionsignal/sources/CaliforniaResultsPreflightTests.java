package com.electionsignal.sources;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
class CaliforniaResultsPreflightTests {
 @Test void precinctsAreNotVotesAndUnknownFormatsFail(){
   var candidate=Map.of("Name","Fictional Candidate","Votes","0");
   var row=Map.of("raceTitle","Governor - Statewide Results","Reporting","0% (0 of 23,860) precincts reporting","ReportingTime","Example date, timezone not supplied","candidates",List.of(candidate));
   var status=CaliforniaResultsPreflight.inspect(List.of(row));assertEquals("NOT_REPORTING",status.availability());assertEquals(0,status.precinctPercent());
   var counting=new HashMap<>(row);counting.put("candidates",List.of(Map.of("Name","Fictional Candidate","Votes","10")));assertEquals("SOURCE_UNVERIFIED",CaliforniaResultsPreflight.inspect(List.of(counting)).availability());
   var invalid=new HashMap<>(row);invalid.put("Reporting","10% estimated votes counted");assertThrows(IllegalArgumentException.class,()->CaliforniaResultsPreflight.inspect(List.of(invalid)));
 }
}
