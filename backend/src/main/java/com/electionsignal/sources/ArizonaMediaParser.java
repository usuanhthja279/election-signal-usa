package com.electionsignal.sources;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import tools.jackson.databind.ObjectMapper;

/** Schema-only parser: external identities stay unmapped and the result is never published here. */
public final class ArizonaMediaParser {
    private static final int MAX_BYTES=10*1024*1024;
    private final ObjectMapper mapper;
    public ArizonaMediaParser(ObjectMapper mapper){this.mapper=mapper;}
    public record Candidate(String sourceId,String name,String party,Long votes){}
    public record Race(String scope,String localityId,String localityName,String sourceId,String name,
                       Integer precinctsReporting,Integer precinctsParticipating,List<Candidate> candidates){}
    public record Snapshot(LocalDate electionDate,Instant createdAt,List<Race> races){}
    private static String text(Object value,String field){if(!(value instanceof String s)||s.isBlank()||s.length()>300)throw new IllegalArgumentException("Missing or invalid "+field);return s;}
    private static String id(Object value,String field){if(value instanceof String s&&s.matches("[A-Za-z0-9_-]{1,100}"))return s;if(value instanceof Number n){try{return new BigDecimal(n.toString()).toBigIntegerExact().toString();}catch(ArithmeticException ignored){}}throw new IllegalArgumentException("Invalid "+field);}
    private static Long count(Object value,String field){if(value==null)return null;if(!(value instanceof Number n))throw new IllegalArgumentException("Invalid "+field);try{long x=new BigDecimal(n.toString()).longValueExact();if(x<0)throw new IllegalArgumentException("Negative "+field);return x;}catch(ArithmeticException ex){throw new IllegalArgumentException("Invalid "+field,ex);}}
    @SuppressWarnings("unchecked") public Snapshot parse(byte[] bytes){
        if(bytes==null||bytes.length==0||bytes.length>MAX_BYTES)throw new IllegalArgumentException("Invalid payload size");
        Map<String,Object> document=mapper.readValue(bytes,Map.class);LocalDate day=LocalDate.parse(text(document.get("electionDate"),"electionDate"));
        if(!day.equals(LocalDate.of(2026,11,3)))throw new IllegalArgumentException("Wrong election date");
        Instant created=Instant.parse(text(document.get("createdAt"),"createdAt"));
        var races=new ArrayList<Race>();var seen=new HashSet<String>();
        Object statewide=document.get("results");if(!(statewide instanceof Map<?,?> state))throw new IllegalArgumentException("Missing statewide results");
        parseArea(state,"STATE",null,"Arizona",races,seen);
        Object local=document.get("localResults");if(!(local instanceof List<?> areas))throw new IllegalArgumentException("Missing local results");
        for(Object value:areas){if(!(value instanceof Map<?,?> area))throw new IllegalArgumentException("Invalid locality");parseArea(area,"LOCAL",id(area.get("id"),"locality id"),text(area.get("name"),"locality name"),races,seen);}
        return new Snapshot(day,created,List.copyOf(races));
    }
    private static void parseArea(Map<?,?> area,String scope,String localityId,String localityName,List<Race> out,Set<String> seen){
        Object value=area.get("ballotItems");if(!(value instanceof List<?> items))throw new IllegalArgumentException("Missing ballot items");
        for(Object item:items){if(!(item instanceof Map<?,?> race))throw new IllegalArgumentException("Invalid ballot item");String raceId=id(race.get("id"),"contest id"),name=text(race.get("name"),"contest name");
            if(!seen.add(scope+"|"+localityId+"|"+raceId))throw new IllegalArgumentException("Duplicate scoped contest");
            Long reported=scope.equals("LOCAL")?count(race.get("precinctsReporting"),"precincts reporting"):null;
            Long participating=scope.equals("LOCAL")?count(race.get("precinctsParticipating"),"participating precincts"):null;
            if(reported!=null&&participating!=null&&reported>participating)throw new IllegalArgumentException("Precincts reporting exceed participating precincts");
            if(reported!=null&&reported>Integer.MAX_VALUE||participating!=null&&participating>Integer.MAX_VALUE)throw new IllegalArgumentException("Precinct count too large");
            Object options=race.get("ballotOptions");if(!(options instanceof List<?> entries))throw new IllegalArgumentException("Missing ballot options");
            var candidates=new ArrayList<Candidate>();var optionIds=new HashSet<String>();
            for(Object entry:entries){if(!(entry instanceof Map<?,?> option))throw new IllegalArgumentException("Invalid option");String optionId=id(option.get("id"),"option id");if(!optionIds.add(optionId))throw new IllegalArgumentException("Duplicate option id");String party=option.get("politicalParty") instanceof String p?p:null;candidates.add(new Candidate(optionId,text(option.get("name"),"option name"),party,count(option.get("voteCount"),"votes")));}
            out.add(new Race(scope,localityId,localityName,raceId,name,reported==null?null:reported.intValue(),participating==null?null:participating.intValue(),List.copyOf(candidates)));
        }
    }
}
