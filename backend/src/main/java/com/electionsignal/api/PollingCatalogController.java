package com.electionsignal.api;

import java.sql.SQLException;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

/** Read-only catalog. Provider configuration is not treated as a successful connection. */
@RestController
@RequestMapping("/api/v1")
@Profile("local")
public class PollingCatalogController {
  private final DataSource database;
  public PollingCatalogController(DataSource database){this.database=database;}
  private Map<String,Object> envelope(Object data){var source=new LinkedHashMap<String,Object>();source.put("provider",null);source.put("sourceAsOf",null);source.put("attribution","Local polling source registry");var result=new LinkedHashMap<String,Object>();result.put("schemaVersion","1.0");result.put("dataMode","POLL");result.put("isTestData",false);result.put("status","NOT_CONNECTED");result.put("source",source);result.put("data",data);return result;}
  @GetMapping({"/polls/providers","/polling/sources/health"}) public Object providers() throws SQLException {
    var items=new ArrayList<Map<String,Object>>();try(var c=database.getConnection();var q=c.prepareStatement("SELECT provider_id,display_name,website_url,enabled,connection_status,last_attempt,last_success,last_publication,consecutive_errors FROM polling_providers ORDER BY display_name");var r=q.executeQuery()) {while(r.next()){var item=new LinkedHashMap<String,Object>();item.put("providerId",r.getString(1));item.put("name",r.getString(2));item.put("website",r.getString(3));item.put("enabled",r.getBoolean(4));item.put("status",r.getString(5));item.put("lastAttempt",r.getTimestamp(6)==null?null:r.getTimestamp(6).toInstant().toString());item.put("lastSuccess",r.getTimestamp(7)==null?null:r.getTimestamp(7).toInstant().toString());item.put("lastPublication",r.getTimestamp(8)==null?null:r.getTimestamp(8).toInstant().toString());item.put("consecutiveErrors",r.getInt(9));items.add(item);}}return envelope(items);
  }
  @GetMapping("/polls/pollsters") public Object pollsters() throws SQLException {
    var items=new ArrayList<Map<String,Object>>();try(var c=database.getConnection();var q=c.prepareStatement("SELECT DISTINCT t.pollster_id,t.display_name FROM pollsters t JOIN poll_surveys p ON p.pollster_id=t.pollster_id JOIN poll_publications pp ON pp.survey_id=p.survey_id JOIN polling_sources s ON s.source_id=pp.source_id WHERE s.review_status='APPROVED' AND s.redistribution_allowed=true ORDER BY t.display_name,t.pollster_id LIMIT 500");var r=q.executeQuery()) {while(r.next())items.add(Map.of("id",r.getString(1),"name",r.getString(2)));}return envelope(items);
  }
}
