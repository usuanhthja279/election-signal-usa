package com.electionsignal.api;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.ObjectMapper;

/** Read-only Phase 0-compatible fixtures. This controller exists only in the demo profile. */
@RestController
@RequestMapping("/api/v1")
@Profile("demo")
public class DemoApiController {
    private static final Set<String> STATES = Set.of("AL","AK","AZ","AR","CA","CO","CT","DE","DC","FL","GA","HI","ID","IL","IN","IA","KS","KY","LA","ME","MD","MA","MI","MN","MS","MO","MT","NE","NV","NH","NJ","NM","NY","NC","ND","OH","OK","OR","PA","RI","SC","SD","TN","TX","UT","VT","VA","WA","WV","WI","WY");
    private static final Set<String> OFFICES = Set.of("all", "house", "senate", "governor");
    private final List<Map<String,Object>> races;
    private final Object meta;
    private final Object sources;
    private final Map<String,Object> geography;

    @SuppressWarnings("unchecked")
    public DemoApiController(ObjectMapper mapper, @Value("${election.fixtures-dir:../data}") String directory) throws IOException {
        Path dir = Path.of(directory);
        races = mapper.readValue(dir.resolve("demo/races.json").toFile(), List.class);
        meta = mapper.readValue(dir.resolve("demo/meta.json").toFile(), Object.class);
        sources = mapper.readValue(dir.resolve("demo/sources.json").toFile(), Object.class);
        geography = mapper.readValue(dir.resolve("geography/reference-boundaries.json").toFile(), Map.class);
    }

    private Map<String,Object> envelope(String status, Object data) {
        Map<String,Object> source = new LinkedHashMap<>();
        source.put("provider", "phase0-fixture"); source.put("sourceAsOf", null);
        source.put("attribution", "Fictional fixture data - not an election data provider");
        Map<String,Object> body = new LinkedHashMap<>();
        body.put("schemaVersion", "1.0"); body.put("dataMode", "DEMO"); body.put("isTestData", true);
        body.put("status", status); body.put("source", source); body.put("data", data);
        return body;
    }
    private String state(String value) {
        if (value == null || value.isBlank()) return null;
        String result = value.toUpperCase(Locale.ROOT);
        if (!STATES.contains(result)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown state abbreviation");
        return result;
    }
    private String office(String value) {
        String result = value.toLowerCase(Locale.ROOT);
        if (!OFFICES.contains(result)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid office");
        return result;
    }
    private int historicalYear(int year) {
        if (year < 2000 || year > 2024 || year % 2 != 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid historical election year");
        return year;
    }
    private void scope(Map<String,String> params) {
        String selectedOffice=office(params.getOrDefault("office","all"));
        String selectedState=state(params.get("state")); String district=params.get("district"); String provider=params.get("provider");
        if(district!=null && (selectedState==null || !selectedOffice.equals("house") || !district.matches("[0-9]{2}"))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid district context");
        if(provider!=null && !provider.matches("[A-Za-z0-9_-]{1,40}")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid provider");
    }
    @GetMapping("/meta") public Object meta() { return envelope("AVAILABLE_DEMO", meta); }
    @GetMapping("/sources/health") public Object sources() { return envelope("NOT_CONNECTED", sources); }
    @GetMapping("/context") public Object context(@RequestParam(defaultValue="live") String mode, @RequestParam(defaultValue="all") String office, @RequestParam(required=false) String state, @RequestParam(required=false) Integer year) {
        if (!Set.of("live","history","polls","exit").contains(mode)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid mode");
        String chosenOffice = office(office); String chosenState = state(state);
        int chosenYear = mode.equals("history") ? historicalYear(year == null ? 2024 : year) : 2026;
        String status = switch (mode) { case "history" -> "NOT_IMPORTED"; case "polls" -> "NOT_CONNECTED"; case "exit" -> "NOT_PUBLISHED"; default -> "DEMO_ONLY"; };
        String category = switch (mode) { case "history" -> "certified_historical_results"; case "polls" -> "pre_election_surveys"; case "exit" -> "election_day_voter_surveys"; default -> "reported_ballots"; };
        Map<String,Object> data = new LinkedHashMap<>();
        data.put("mode", mode); data.put("office", chosenOffice); data.put("state", chosenState); data.put("year", chosenYear);
        data.put("status", status); data.put("category", category); data.put("availableModes", List.of("live","history","polls","exit")); data.put("forecastModelAvailable", false);
        return envelope(status, data);
    }
    @GetMapping("/demo/races") public Object races(@RequestParam(defaultValue="all") String office, @RequestParam(required=false) String state, @RequestParam Map<String,String> params) {
        scope(params);
        String selectedOffice = office(office), selectedState = state(state);
        String selectedDistrict=params.get("district");
        List<Map<String,Object>> filtered = races.stream().filter(r -> (selectedOffice.equals("all") || selectedOffice.equals(r.get("office"))) && (selectedState == null || selectedState.equals(r.get("state"))) && (selectedDistrict==null || selectedDistrict.equals(r.get("district")))).toList();
        Map<String,Object> body = envelope("AVAILABLE_DEMO", filtered); body.put("total", filtered.size()); return body;
    }
    @GetMapping("/demo/races/{id}") public Object race(@PathVariable String id) {
        if (!id.matches("[A-Za-z0-9_-]{1,80}")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid race ID");
        return races.stream().filter(r -> id.equals(r.get("id"))).findFirst().map(r -> envelope("AVAILABLE_DEMO", r)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Race not found"));
    }
    @GetMapping("/demo/states/{state}/summary") public Object stateSummary(@PathVariable String state) {
        String st = state(state); var selected = races.stream().filter(r -> st.equals(r.get("state"))).toList();
        return envelope("AVAILABLE_DEMO", Map.of("state", st, "contests", selected, "offices", List.of("senate","governor","house"), "hasSampleData", !selected.isEmpty()));
    }
    @SuppressWarnings("unchecked")
    @GetMapping("/geography/manifest") public Object manifest() {
        Map<String,Object> states = (Map<String,Object>) geography.get("states");
        Map<String,List<Object>> counties = (Map<String,List<Object>>) geography.get("counties");
        return envelope("REFERENCE_ONLY", Map.of("states", states.size(), "counties", counties.values().stream().mapToInt(List::size).sum(), "boundaryVersion", "V3 prototype reference; not verified 2026 legal district map", "eligibleFor2026HouseDistrictAssignments", false));
    }
    @GetMapping("/geography/states") public Object states() {
        return envelope("REFERENCE_ONLY", geography.get("states"));
    }
    @SuppressWarnings("unchecked")
    @GetMapping("/geography/states/{state}/counties") public Object counties(@PathVariable String state) {
        String st = state(state); Map<String,List<Object>> counties = (Map<String,List<Object>>) geography.get("counties");
        List<Object> shapes = counties.getOrDefault(st, List.of()); return envelope("REFERENCE_ONLY", Map.of("state", st, "countyCount", shapes.size(), "countyShapes", shapes));
    }
    @GetMapping("/history/results") public Object history(@RequestParam(defaultValue="2024") int year, @RequestParam Map<String,String> params) {
        scope(params);
        Map<String,Object> body = envelope("NOT_IMPORTED", List.of()); body.put("year", historicalYear(year)); body.put("notice", "No verified historical returns imported."); return body;
    }
    @GetMapping("/polls") public Object polls(@RequestParam Map<String,String> params) { scope(params);return envelope("NOT_CONNECTED", List.of()); }
    @GetMapping("/polls/averages") public Object averages(@RequestParam Map<String,String> params) { scope(params);return envelope("NOT_CONNECTED", List.of()); }
    @GetMapping("/forecasts/published") public Object forecasts(@RequestParam Map<String,String> params) { scope(params);return envelope("NOT_CONNECTED", List.of()); }
    @GetMapping("/exit-polls") public Object exits(@RequestParam(defaultValue="2026") int year, @RequestParam Map<String,String> params) {
        scope(params);
        if(year<2000||year>2026||year%2!=0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid survey year");
        var body=envelope("NOT_PUBLISHED",List.of());body.put("year",year);return body;
    }
    @GetMapping("/models/races/{id}") public Object model(@PathVariable String id) {
        if (!id.matches("[A-Za-z0-9_-]{1,80}")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid race ID");
        Map<String,Object> body = envelope("UNAVAILABLE_NOT_TRAINED", null); body.put("raceId", id); body.put("modelVersion", null); body.put("probabilities", null); return body;
    }
    @GetMapping(value="/events", produces=MediaType.TEXT_EVENT_STREAM_VALUE) public SseEmitter events() throws IOException {
        SseEmitter emitter = new SseEmitter(25000L);
        emitter.send(SseEmitter.event().name("server_status").data(Map.of("phase",1,"dataMode","DEMO","resultsStream",false,"notice","No live vote events connected")));
        emitter.complete(); return emitter;
    }
}
