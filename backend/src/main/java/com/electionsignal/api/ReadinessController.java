package com.electionsignal.api;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Operational summary only; it never exposes credentials or implies data readiness. */
@RestController
@RequestMapping("/api/v1")
@Profile("local")
public class ReadinessController {
    private final DataSource database;
    public ReadinessController(DataSource database) { this.database = database; }
    private long count(String sql) throws SQLException { try(var c=database.getConnection();var q=c.createStatement();var r=q.executeQuery(sql)){r.next();return r.getLong(1);} }
    private Map<String,Object> envelope(String status,Object data) {
        var source=new LinkedHashMap<String,Object>();source.put("provider",null);source.put("sourceAsOf",null);source.put("attribution","Local registry readiness; no external provider fetched");
        var body=new LinkedHashMap<String,Object>();body.put("schemaVersion","1.0");body.put("dataMode","ELECTION_REGISTRY");body.put("isTestData",false);body.put("status",status);body.put("source",source);body.put("data",data);return body;
    }
    @GetMapping("/ops/readiness") public Object readiness() throws SQLException {
        long migrations=count("SELECT count(*) FROM flyway_schema_history WHERE success=true");
        long enabledProviders=count("SELECT count(*) FROM source_providers WHERE enabled=true AND use_rights_verified=true AND license_id IS NOT NULL");
        long contests=count("SELECT count(*) FROM races r JOIN elections e USING(election_id) WHERE e.cycle_year=2026");
        long districts=count("SELECT count(*) FROM congressional_districts WHERE verification_status='VERIFIED'");
        long history=count("SELECT count(*) FROM historical_result_rows r JOIN source_publications p USING(publication_id) WHERE p.legal_review_status='APPROVED' AND p.redistribution_allowed=true");
        var checks=new ArrayList<Map<String,Object>>();
        checks.add(check("Database migrations",migrations>=4?"PASS":"BLOCKED",migrations+" successful migrations"));
        checks.add(check("Licensed/enabled provider",enabledProviders>0?"PASS":"BLOCKED",enabledProviders+" providers approved and enabled"));
        checks.add(check("2026 contest registry",contests>0?"PARTIAL":"BLOCKED",contests+" reviewed contests"));
        checks.add(check("2026 House geography",districts==435?"PASS":"BLOCKED",districts+" verified voting districts; expected 435"));
        checks.add(check("Certified historical data",history>0?"PARTIAL":"BLOCKED",history+" redistributable result rows"));
        checks.add(check("Live election feed",enabledProviders>0?"REVIEW_REQUIRED":"NOT_CONNECTED","No provider health verification has run"));
        var data=new LinkedHashMap<String,Object>();data.put("overallStatus","BLOCKED");data.put("checks",checks);data.put("publicReportingEnabled",false);
        data.put("readinessCounts",Map.of("successfulMigrations",migrations,"enabledProviders",enabledProviders,"registryContests",contests,"verifiedHouseDistricts",districts,"redistributableHistoryRows",history));
        return envelope("BLOCKED",data);
    }
    private Map<String,Object> check(String name,String status,String detail){return Map.of("name",name,"status",status,"detail",detail);}

    @GetMapping("/sources/inventory") public Object sourceInventory() throws SQLException {
        var entries=new ArrayList<Map<String,Object>>();
        try(var c=database.getConnection();var q=c.prepareStatement("SELECT provider_id,display_name,source_url,category,license_id,use_rights_verified,enabled,coverage_json FROM source_providers ORDER BY provider_id");var r=q.executeQuery()) {
            while(r.next()){var item=new LinkedHashMap<String,Object>();item.put("providerId",r.getString(1));item.put("name",r.getString(2));item.put("sourceUrl",r.getString(3));item.put("category",r.getString(4));item.put("licenseId",r.getString(5));item.put("rightsVerified",r.getBoolean(6));item.put("enabled",r.getBoolean(7));item.put("coverage",r.getString(8));item.put("status",r.getBoolean(7)&&r.getBoolean(6)?"CONFIGURED":"NOT_CONNECTED");entries.add(item);}
        }
        return envelope("AVAILABLE",entries);
    }

    @GetMapping("/history/coverage") public Object historyCoverage() throws SQLException {
        var available=new ArrayList<Map<String,Object>>();
        String sql="SELECT c.election_year,c.office_code,count(DISTINCT c.contest_id) contests,count(r.result_row_id) result_rows,count(DISTINCT r.geo_type) geography_types FROM historical_contests c JOIN source_publications p USING(publication_id) LEFT JOIN historical_result_rows r ON r.contest_id=c.contest_id AND r.publication_id=p.publication_id WHERE c.result_status IN ('CERTIFIED','UNCONTESTED') AND p.legal_review_status='APPROVED' AND p.redistribution_allowed=true GROUP BY c.election_year,c.office_code ORDER BY c.election_year,c.office_code";
        try(var c=database.getConnection();var q=c.prepareStatement(sql);var r=q.executeQuery()){while(r.next()){var row=new LinkedHashMap<String,Object>();row.put("year",r.getInt(1));row.put("office",r.getString(2));row.put("contests",r.getLong(3));row.put("resultRows",r.getLong(4));row.put("geographyTypes",r.getLong(5));row.put("status","AVAILABLE");available.add(row);}}
        for(int year=2000;year<=2024;year+=2){final int y=year;if(available.stream().noneMatch(row->((Number)row.get("year")).intValue()==y))available.add(Map.of("year",year,"status","NOT_IMPORTED","contests",0,"resultRows",0,"geographyTypes",0));}
        available.sort((a,b)->{int y=Integer.compare(((Number)a.get("year")).intValue(),((Number)b.get("year")).intValue());return y!=0?y:String.valueOf(a.get("office")).compareTo(String.valueOf(b.get("office")));});
        var body=envelope(available.stream().anyMatch(row->"AVAILABLE".equals(row.get("status")))?"PARTIAL":"NOT_IMPORTED",available);
        body.put("dataMode","HISTORICAL_CERTIFIED");
        return body;
    }
}
