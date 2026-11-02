package com.electionsignal.api;

import com.electionsignal.polling.PollingReadService;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.ObjectMapper;

/** Non-demo API: no fixtures, no unverified external data and no invented results. */
@RestController
@RequestMapping("/api/v1")
@Profile("local")
public class LocalApiController {
    private static final Set<String> STATES = Set.of("AL","AK","AZ","AR","CA","CO","CT","DE","DC","FL","GA","HI","ID","IL","IN","IA","KS","KY","LA","ME","MD","MA","MI","MN","MS","MO","MT","NE","NV","NH","NJ","NM","NY","NC","ND","OH","OK","OR","PA","RI","SC","SD","TN","TX","UT","VT","VA","WA","WV","WI","WY");
    private final DataSource database;
    private final PollingReadService polling;
    private final Map<String,Object> geography;
    @SuppressWarnings("unchecked")
    public LocalApiController(DataSource database, PollingReadService polling, ObjectMapper mapper, @Value("${election.fixtures-dir:../data}") String directory) throws IOException {
        this.database=database;
        this.polling=polling;
        geography=mapper.readValue(Path.of(directory,"geography/reference-boundaries.json").toFile(),Map.class);
    }
    private Map<String,Object> envelope(String mode,String status,Object data) {
        Map<String,Object> source=new LinkedHashMap<>(); source.put("provider",null); source.put("sourceAsOf",null); source.put("attribution","No verified election provider connected");
        Map<String,Object> body=new LinkedHashMap<>(); body.put("schemaVersion","1.0"); body.put("dataMode",mode); body.put("isTestData",false); body.put("status",status); body.put("source",source); body.put("data",data); return body;
    }
    private Map<String,Object> reference(Object data) {
        var body=envelope("REFERENCE_GEOGRAPHY","REFERENCE_ONLY",data);
        Map<String,Object> source=new LinkedHashMap<>();source.put("provider","prototype-reference-geography");source.put("sourceAsOf",null);source.put("attribution","Historical reference boundaries; not verified for 2026 districts. See licenses and limitations.");
        body.put("source",source);return body;
    }
    private String state(String value) {
        if(value==null||value.isBlank()) return null;
        String result=value.toUpperCase(Locale.ROOT); if(!STATES.contains(result)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unknown state abbreviation"); return result;
    }
    private String office(String value) {
        String result=value.toLowerCase(Locale.ROOT); if(!Set.of("all","house","senate","governor").contains(result)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid office"); return result;
    }
    private int year(int value) { if(value<2000||value>2024||value%2!=0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid historical election year"); return value; }
    private void scope(Map<String,String> params) throws SQLException {
        String selectedOffice=office(params.getOrDefault("office","all"));String selectedState=state(params.get("state"));String district=params.get("district"),provider=params.get("provider");
        if(district!=null&&(selectedState==null||!selectedOffice.equals("house")||!district.matches("[0-9]{2}"))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid district context");
        if(district!=null)try(var connection=database.getConnection();var query=connection.prepareStatement("SELECT 1 FROM congressional_districts WHERE state_code=? AND district_code=? AND geography_version_id='CENSUS_BAS_2026_CD119' AND verification_status='VERIFIED'")){query.setString(1,selectedState);query.setString(2,district);try(var rows=query.executeQuery()){if(!rows.next())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"District is not in the verified election registry");}}
        if(provider!=null&&!provider.matches("[A-Za-z0-9_-]{1,40}")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid provider");
    }
    @GetMapping("/health") public Object health() throws SQLException {
        try(Connection connection=database.getConnection()) {
            if(!connection.isValid(2)) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Database unavailable");
        }
        return Map.of("service","election-signal-phase1","status","ok","phase",1,"dataMode","LIVE_REPORTED","isTestData",false,"connected",Map.of("live",false,"history",false,"polls",false,"exitPolls",false,"forecastModel",false));
    }
    @GetMapping("/meta") public Object meta() {
        return envelope("LIVE_REPORTED","AVAILABLE",Map.of("project","Election Signal","phase",1,"cycle",2026,"electionDate","2026-11-03","reportingTimeZone","America/New_York","globalModes",List.of(Map.of("id","live","label","Live Results"),Map.of("id","history","label","Past Results"),Map.of("id","polls","label","Polls & Forecasts"),Map.of("id","exit","label","Exit Polls")),"historicalYears",java.util.stream.IntStream.rangeClosed(0,12).map(i->2000+i*2).boxed().toList(),"primaryModelBaselineYears",List.of(2022,2024),"modelStatus","UNAVAILABLE_NOT_TRAINED"));
    }
    @GetMapping("/sources/health") public Object sources() throws SQLException {
        List<Map<String,Object>> items=new ArrayList<>();
        try(var connection=database.getConnection(); var query=connection.prepareStatement("SELECT provider_id,display_name,category,source_url,use_rights_verified,enabled,license_id FROM source_providers ORDER BY provider_id"); var rows=query.executeQuery()) {
            while(rows.next()) { Map<String,Object> item=new LinkedHashMap<>(); item.put("id",rows.getString(1)); item.put("name",rows.getString(2)); item.put("category",rows.getString(3)); item.put("url",rows.getString(4)); item.put("licenseVerified",rows.getBoolean(5)); item.put("rightsStatus", rows.getBoolean(5)&&rows.getString(7)!=null?"VERIFIED":"UNVERIFIED"); item.put("connection",rows.getBoolean(6)?"CONFIGURED_UNVERIFIED":"NOT_CONNECTED"); item.put("coverageVerified",false); items.add(item); }
        }
        return envelope("LIVE_REPORTED","NOT_CONNECTED",items);
    }
    @GetMapping("/context") public Object context(@RequestParam(defaultValue="live") String mode,@RequestParam(defaultValue="all") String office,@RequestParam(required=false) String state,@RequestParam(required=false) Integer year) {
        if(!Set.of("live","history","polls","exit").contains(mode)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid mode");
        String status=switch(mode){case "history"->"NOT_IMPORTED";case "exit"->"NOT_PUBLISHED";default->"NOT_CONNECTED";};
        String dataMode=switch(mode){case "history"->"HISTORICAL_CERTIFIED";case "polls"->"POLL";case "exit"->"EXIT_SURVEY";default->"LIVE_REPORTED";};
        String category=switch(mode){case "history"->"certified_historical_results";case "polls"->"pre_election_surveys";case "exit"->"election_day_voter_surveys";default->"reported_ballots";};
        Map<String,Object> data=new LinkedHashMap<>(); data.put("mode",mode);data.put("office",office(office));data.put("state",state(state));data.put("year",mode.equals("history")?year(year==null?2024:year):2026);data.put("status",status);data.put("category",category);data.put("availableModes",List.of("live","history","polls","exit"));data.put("forecastModelAvailable",false);
        return envelope(dataMode,status,data);
    }
    @GetMapping("/live/races") public Object live(@RequestParam Map<String,String> params) throws SQLException { scope(params);return envelope("LIVE_REPORTED","NOT_CONNECTED",List.of()); }
    @GetMapping("/history/results") public Object history(@RequestParam(defaultValue="2024") int year,@RequestParam(defaultValue="0") int offset,@RequestParam(defaultValue="100") int limit,@RequestParam Map<String,String> params) throws SQLException {
        scope(params);int selectedYear=year(year);if(offset<0||limit<1||limit>500)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid pagination; limit must be 1–500");
        String selectedOffice=office(params.getOrDefault("office","all"));String officeCode=selectedOffice.equals("all")?null:selectedOffice.toUpperCase(Locale.ROOT);String selectedState=state(params.get("state"));String district=params.get("district");
        String sql="SELECT c.contest_id,c.election_year,c.office_code,c.state_code,c.district_code,c.seat_class,c.election_subtype,c.result_status,c.certification_authority,c.certification_at,p.provider_id,p.title,p.source_url,p.sha256,p.source_as_of,r.geo_id,r.geo_type,r.candidate_id,r.candidate_name,r.party_code,r.vote_method,r.votes,r.geography_version_id "
                +"FROM historical_contests c JOIN source_publications p USING(publication_id) LEFT JOIN historical_result_rows r ON r.contest_id=c.contest_id AND r.publication_id=p.publication_id "
                +"WHERE c.election_year=? AND p.legal_review_status='APPROVED' AND p.redistribution_allowed=true AND (? IS NULL OR c.office_code=?) AND (? IS NULL OR c.state_code=?) AND (? IS NULL OR c.district_code=?) "
                +"ORDER BY c.state_code,c.office_code,c.district_code,c.seat_class,c.contest_id,r.geo_type,r.geo_id,r.candidate_id OFFSET ? LIMIT ?";
        List<Map<String,Object>> records=new ArrayList<>();
        try(var connection=database.getConnection();var query=connection.prepareStatement(sql)){query.setInt(1,selectedYear);query.setString(2,officeCode);query.setString(3,officeCode);query.setString(4,selectedState);query.setString(5,selectedState);query.setString(6,district);query.setString(7,district);query.setInt(8,offset);query.setInt(9,limit);try(var rows=query.executeQuery()){while(rows.next()){Map<String,Object> item=new LinkedHashMap<>();item.put("contestId",rows.getString(1));item.put("year",rows.getInt(2));item.put("office",rows.getString(3));item.put("state",rows.getString(4).trim());item.put("district",rows.getString(5));item.put("seatClass",rows.getString(6));item.put("subtype",rows.getString(7));item.put("resultStatus",rows.getString(8));item.put("certifyingAuthority",rows.getString(9));item.put("certifiedAt",rows.getTimestamp(10)==null?null:rows.getTimestamp(10).toInstant().toString());item.put("sourceProvider",rows.getString(11));item.put("sourceTitle",rows.getString(12));item.put("sourceUrl",rows.getString(13));item.put("sourceSha256",rows.getString(14));item.put("sourceAsOf",rows.getTimestamp(15)==null?null:rows.getTimestamp(15).toInstant().toString());item.put("geoId",rows.getString(16));item.put("geoType",rows.getString(17));item.put("candidateId",rows.getString(18));item.put("candidateName",rows.getString(19));item.put("party",rows.getString(20));item.put("voteMethod",rows.getString(21));long votes=rows.getLong(22);item.put("votes",rows.wasNull()?null:votes);item.put("geographyVersion",rows.getString(23));records.add(item);}}}
        var body=envelope("HISTORICAL_CERTIFIED",records.isEmpty()?"NOT_IMPORTED":"AVAILABLE",records);body.put("year",selectedYear);body.put("offset",offset);body.put("limit",limit);return body;
    }
    @GetMapping("/polls") public Object polls(@RequestParam Map<String,String> params)throws SQLException{scope(params);return polling.polls(params);}
    @SuppressWarnings("unchecked") @GetMapping("/polls/{id}") public Object pollDetail(@PathVariable String id,@RequestParam Map<String,String> params)throws SQLException{scope(params);var query=new HashMap<>(params);query.put("pollId",id);var body=(Map<String,Object>)polling.polls(query);var rows=(List<?>)body.get("data");if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Poll not found");body.put("data",rows.get(0));return body;}
    @GetMapping({"/polls/averages","/polls/trends"}) public Object averages(@RequestParam Map<String,String> params)throws SQLException{scope(params);return polling.averages(params);}
    @GetMapping("/forecasts/published") public Object forecasts(@RequestParam Map<String,String> params)throws SQLException{scope(params);return polling.forecasts(params);}
    @SuppressWarnings("unchecked") @GetMapping("/forecasts/published/{id}") public Object forecastDetail(@PathVariable String id,@RequestParam Map<String,String> params)throws SQLException{scope(params);var query=new HashMap<>(params);query.put("forecastId",id);var body=(Map<String,Object>)polling.forecasts(query);var rows=(List<?>)body.get("data");if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Forecast not found");body.put("data",rows.get(0));return body;}
    @GetMapping("/exit-polls") public Object exits(@RequestParam(defaultValue="2026") int year,@RequestParam Map<String,String> params)throws SQLException{
        scope(params);
        if(year<2000||year>2026||year%2!=0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid survey year");
        var body=envelope("EXIT_SURVEY","NOT_PUBLISHED",List.of());body.put("year",year);return body;
    }
    @GetMapping("/models/races/{id}") public Object model(@PathVariable String id){
        if(!id.matches("[A-Za-z0-9_-]{1,80}")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid race ID");
        var body=envelope("INTERNAL_MODEL","UNAVAILABLE_NOT_TRAINED",null);body.put("raceId",id);body.put("modelVersion",null);body.put("probabilities",null);return body;
    }
    @SuppressWarnings("unchecked") @GetMapping("/geography/manifest") public Object manifest(){
        var states=(Map<String,Object>)geography.get("states");var counties=(Map<String,List<Object>>)geography.get("counties");
        return reference(Map.of("states",states.size(),"counties",counties.values().stream().mapToInt(List::size).sum(),"boundaryVersion","Prototype reference; not verified for 2026","eligibleFor2026HouseDistrictAssignments",false));
    }
    @GetMapping("/geography/states") public Object states(){return reference(geography.get("states"));}
    @SuppressWarnings("unchecked") @GetMapping("/geography/states/{state}/counties") public Object counties(@PathVariable String state){
        String st=state(state);var all=(Map<String,List<Object>>)geography.get("counties");var shapes=all.getOrDefault(st,List.of());return reference(Map.of("state",st,"countyCount",shapes.size(),"countyShapes",shapes));
    }
    @GetMapping(value="/events",produces=MediaType.TEXT_EVENT_STREAM_VALUE) public SseEmitter events() throws IOException {
        SseEmitter emitter=new SseEmitter(25000L);emitter.send(SseEmitter.event().name("server_status").data(Map.of("phase",1,"dataMode","LIVE_REPORTED","resultsStream",false,"notice","No live vote events connected")));emitter.complete();return emitter;
    }
}
