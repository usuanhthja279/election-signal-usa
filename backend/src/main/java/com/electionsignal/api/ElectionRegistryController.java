package com.electionsignal.api;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Read-only 2026 registry API. Empty race/district data is explicit, never guessed. */
@RestController
@RequestMapping("/api/v1/elections/2026")
@Profile("local")
public class ElectionRegistryController {
    private static final Set<String> OFFICES = Set.of("HOUSE", "SENATE", "GOVERNOR");
    private final DataSource database;
    public ElectionRegistryController(DataSource database) { this.database = database; }

    private Map<String,Object> envelope(String status, Object data, String provider, String attribution) {
        var source = new LinkedHashMap<String,Object>(); source.put("provider", provider);
        source.put("sourceAsOf", null); source.put("attribution", attribution);
        var result = new LinkedHashMap<String,Object>(); result.put("schemaVersion", "1.0");
        result.put("dataMode", "ELECTION_REGISTRY"); result.put("isTestData", false);
        result.put("status", status); result.put("source", source); result.put("data", data);
        return result;
    }

    @GetMapping("/registry") public Object registry() throws SQLException {
        long races = count("SELECT count(*) FROM races r JOIN elections e USING (election_id) WHERE e.cycle_year=2026");
        long districts = count("SELECT count(*) FROM congressional_districts WHERE geography_version_id='CENSUS_BAS_2026_CD119' AND verification_status='VERIFIED'");
        long mapped = count("SELECT count(*) FROM external_race_mappings WHERE mapping_status='VERIFIED'");
        return envelope("AVAILABLE", Map.of(
                "electionId", "US-2026-GENERAL", "year", 2026, "date", "2026-11-03", "kind", "GENERAL",
                "registryStatus", races > 0 ? "PARTIAL" : "NOT_IMPORTED", "raceCount", races,
                "verifiedHouseDistrictCount", districts, "verifiedExternalMappingCount", mapped,
                "supportedOffices", List.of("HOUSE", "SENATE", "GOVERNOR"),
                "geographyVersion", "CENSUS_BAS_2026_CD119"),
                "U.S. Census Bureau / election authority registry", "Registry metadata only; no ballot contests are certified or imported");
    }

    @GetMapping("/states") public Object states() throws SQLException {
        var data = new ArrayList<Map<String,Object>>();
        try (var c = database.getConnection(); var q = c.prepareStatement("SELECT state_code,state_fips,display_name,jurisdiction_type,source_status FROM jurisdictions ORDER BY display_name"); var r = q.executeQuery()) {
            while (r.next()) { var row = new LinkedHashMap<String,Object>(); row.put("state", r.getString(1).trim()); row.put("fips", r.getString(2)); row.put("name", r.getString(3)); row.put("type", r.getString(4)); row.put("registryStatus", r.getString(5)); data.add(row); }
        }
        return envelope("AVAILABLE", data, "U.S. Census Bureau", "ANSI/FIPS jurisdiction codes; this is not a race-on-ballot claim");
    }

    @GetMapping("/races") public Object races(@RequestParam(required=false) String state,
            @RequestParam(required=false) String office, @RequestParam(defaultValue="0") int offset,
            @RequestParam(defaultValue="100") int limit) throws SQLException {
        validatePage(offset, limit); String normalizedState = state == null ? null : validateState(state);
        String normalizedOffice = office == null ? null : validateOffice(office);
        String sql = "SELECT r.race_id,r.election_id,r.office_code,r.state_code,r.district_code,r.seat_class,r.election_subtype,r.race_status,r.ballot_status "
                + "FROM races r JOIN elections e USING(election_id) WHERE e.cycle_year=2026 AND (? IS NULL OR r.state_code=?) AND (? IS NULL OR r.office_code=?) "
                + "ORDER BY r.state_code,r.office_code,r.district_code,r.seat_class,r.race_id OFFSET ? LIMIT ?";
        var data = new ArrayList<Map<String,Object>>(); long total;
        try (var c = database.getConnection(); var q = c.prepareStatement(sql)) {
            q.setString(1, normalizedState); q.setString(2, normalizedState); q.setString(3, normalizedOffice); q.setString(4, normalizedOffice); q.setInt(5, offset); q.setInt(6, limit);
            try (var r = q.executeQuery()) { while (r.next()) { var row = new LinkedHashMap<String,Object>(); row.put("raceId",r.getString(1)); row.put("electionId",r.getString(2)); row.put("office",r.getString(3)); row.put("state",r.getString(4).trim()); row.put("district",r.getString(5)); row.put("seatClass",r.getString(6)); row.put("subtype",r.getString(7)); row.put("status",r.getString(8)); row.put("ballotStatus",r.getString(9)); data.add(row); } }
            try (var q2 = c.prepareStatement("SELECT count(*) FROM races r JOIN elections e USING(election_id) WHERE e.cycle_year=2026 AND (? IS NULL OR r.state_code=?) AND (? IS NULL OR r.office_code=?)")) {
                q2.setString(1, normalizedState); q2.setString(2, normalizedState); q2.setString(3, normalizedOffice); q2.setString(4, normalizedOffice); try (var r = q2.executeQuery()) { r.next(); total = r.getLong(1); }
            }
        }
        return envelope(total == 0 ? "NOT_IMPORTED" : "AVAILABLE", Map.of("items",data,"offset",offset,"limit",limit,"total",total), "Election registry", "Only contests with a reviewed source are included");
    }

    @GetMapping("/states/{state}/races") public Object stateRaces(@PathVariable String state,
            @RequestParam(required=false) String office, @RequestParam(defaultValue="0") int offset,
            @RequestParam(defaultValue="100") int limit) throws SQLException {
        return races(validateState(state), office, offset, limit);
    }

    @GetMapping("/house/districts") public Object districts(@RequestParam(required=false) String state) throws SQLException {
        String normalizedState = state == null ? null : validateState(state);
        String sql = "SELECT state_code,district_code,geography_version_id,geo_id,display_name,verification_status,source_feature_id FROM congressional_districts WHERE geography_version_id='CENSUS_BAS_2026_CD119' AND (? IS NULL OR state_code=?) ORDER BY state_code,district_code";
        var data = new ArrayList<Map<String,Object>>();
        try (var c = database.getConnection(); var q = c.prepareStatement(sql)) {
            q.setString(1,normalizedState); q.setString(2,normalizedState);
            try (var r = q.executeQuery()) { while (r.next()) { var row = new LinkedHashMap<String,Object>(); row.put("state",r.getString(1).trim()); row.put("district",r.getString(2)); row.put("geographyVersion",r.getString(3)); row.put("geoId",r.getString(4)); row.put("name",r.getString(5)); row.put("verificationStatus",r.getString(6)); row.put("sourceFeatureId",r.getString(7)); data.add(row); } }
        }
        return envelope(data.isEmpty()?"NOT_IMPORTED":"AVAILABLE", Map.of("items",data,"state",normalizedState==null?"ALL":normalizedState,"geographyVersion","CENSUS_BAS_2026_CD119"),
                "U.S. Census Bureau TIGERweb", "119th Congress district source; feature geometry has not been loaded in this installation");
    }

    private long count(String sql) throws SQLException { try (var c=database.getConnection();var q=c.createStatement();var r=q.executeQuery(sql)) { r.next(); return r.getLong(1); } }
    private String validateState(String value) {
        String state=value.toUpperCase(Locale.ROOT);
        if (!state.matches("[A-Z]{2}")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid state code");
        try (var c=database.getConnection();var q=c.prepareStatement("SELECT 1 FROM jurisdictions WHERE state_code=?")) { q.setString(1,state); try(var r=q.executeQuery()){if(!r.next())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unknown state code");} return state; }
        catch (SQLException e) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Registry unavailable",e); }
    }
    private String validateOffice(String value) { String office=value.toUpperCase(Locale.ROOT); if(!OFFICES.contains(office))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid office"); return office; }
    private void validatePage(int offset,int limit) { if(offset<0||limit<1||limit>500)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid pagination; limit must be 1–500"); }
}
