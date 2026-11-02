package com.electionsignal.sources;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
class ArizonaMediaParserTests {
 private final ArizonaMediaParser parser=new ArizonaMediaParser(new ObjectMapper());
 private byte[] payload(String count,String extra){return ("{\"electionDate\":\"2026-11-03\",\"createdAt\":\"2026-11-04T03:10:00Z\",\"results\":{\"name\":\"Arizona\",\"ballotItems\":[{\"id\":\"contest\",\"name\":\"Fictional race\",\"precinctsReporting\":99,\"ballotOptions\":[{\"id\":\"a\",\"name\":\"Fictional A\",\"voteCount\":"+count+"},{\"id\":\"b\",\"name\":\"Fictional B\",\"voteCount\":null}]}]},\"localResults\":[{\"id\":\"county-1\",\"name\":\"Fictional County\",\"ballotItems\":[{\"id\":\"contest\",\"name\":\"Fictional race\",\"precinctsReporting\":1,\"precinctsParticipating\":4,\"ballotOptions\":[{\"id\":\"a\",\"name\":\"Fictional A\",\"voteCount\":0}]}]}]"+extra+"}").getBytes(StandardCharsets.UTF_8);}
 @Test void preservesScopePrecinctsAndCorrectionsWithoutInferringCalls(){
   var a=parser.parse(payload("10",""));var b=parser.parse(payload("7",""));assertEquals(2,a.races().size());assertEquals(10L,a.races().get(0).candidates().get(0).votes());assertEquals(7L,b.races().get(0).candidates().get(0).votes());
   assertNull(a.races().get(0).precinctsReporting());assertNull(a.races().get(0).candidates().get(1).votes());assertEquals(0L,a.races().get(1).candidates().get(0).votes());assertEquals(1,a.races().get(1).precinctsReporting());assertEquals(4,a.races().get(1).precinctsParticipating());assertEquals("county-1",a.races().get(1).localityId());
 }
 @Test void rejectsWrongCycleAndInvalidVotes(){
   assertThrows(IllegalArgumentException.class,()->parser.parse(payload("-1","")));
   var wrong=new String(payload("1",""),StandardCharsets.UTF_8).replace("2026-11-03","2024-11-05");assertThrows(IllegalArgumentException.class,()->parser.parse(wrong.getBytes(StandardCharsets.UTF_8)));
   var duplicate=new String(payload("1",""),StandardCharsets.UTF_8).replace("\"id\":\"b\"","\"id\":\"a\"");assertThrows(IllegalArgumentException.class,()->parser.parse(duplicate.getBytes(StandardCharsets.UTF_8)));
 }
}
