package com.electionsignal.polling;

import java.security.MessageDigest;
import java.sql.*;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import tools.jackson.databind.ObjectMapper;

/** Imports only documented CC-BY publisher forecasts. No poll figures are ingested. */
@Service @Profile("local")
public class PollingForecastImporter {
    private final DataSource db; private final ObjectMapper mapper;
    public PollingForecastImporter(DataSource db,ObjectMapper mapper){this.db=db;this.mapper=mapper;}
    private static String hash(byte[] bytes)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
    private static void run(Connection c,String sql,Object...args)throws SQLException{try(var q=c.prepareStatement(sql)){for(int i=0;i<args.length;i++)q.setObject(i+1,args[i]);q.executeUpdate();}}
    public int importResponse(PollingForecastClient.Response response)throws Exception{
        @SuppressWarnings("unchecked") Map<String,Object> doc=mapper.readValue(response.payload(),Map.class);String kind=(String)doc.get("dataset");PollingForecastClient.validate(kind,doc);
        if(!response.endpoint().equals("https://www.pollingforecast.com/api/us/"+kind))throw new IllegalArgumentException("Unexpected source URL");
        String checksum=hash(response.payload()), date=(String)doc.get("asOf"),office=switch(kind){case "senate"->"SENATE";case "governors"->"GOVERNOR";case "house"->"HOUSE";default->throw new IllegalArgumentException("Unexpected dataset");};
        int inserted=0;
        try(var c=db.getConnection()){
            c.setAutoCommit(false);
            try {
                run(c,"INSERT INTO polling_providers(provider_id,display_name,website_url,enabled,connection_status,last_attempt,last_success) VALUES ('pollingforecast','Polling Forecast','https://www.pollingforecast.com',false,'MANUAL_IMPORT',now(),now()) ON CONFLICT(provider_id) DO UPDATE SET last_attempt=now(),last_success=now(),connection_status='MANUAL_IMPORT'");
                run(c,"INSERT INTO polling_sources(source_id,provider_id,publication_url,methodology_url,license_id,attribution,rights_evidence_url,review_status,redistribution_allowed,reviewed_at) VALUES ('pollingforecast-forecasts','pollingforecast','https://www.pollingforecast.com/us/data','https://www.pollingforecast.com/us/data','CC BY 4.0','Polling Forecast (www.pollingforecast.com); CC BY 4.0','https://www.pollingforecast.com/us/data','APPROVED',true,now()) ON CONFLICT(source_id) DO NOTHING");
                try(var q=c.prepareStatement("SELECT 1 FROM polling_sources WHERE source_id='pollingforecast-forecasts' AND provider_id='pollingforecast' AND publication_url='https://www.pollingforecast.com/us/data' AND rights_evidence_url='https://www.pollingforecast.com/us/data' AND review_status='APPROVED' AND redistribution_allowed=true AND license_id='CC BY 4.0'")){try(var r=q.executeQuery()){if(!r.next())throw new IllegalStateException("Source rights are not approved");}}
                run(c,"INSERT INTO published_forecast_models(model_id,source_id,model_name,methodology_url) VALUES ('pollingforecast-2026','pollingforecast-forecasts','Polling Forecast 2026','https://www.pollingforecast.com/us/data') ON CONFLICT(model_id) DO NOTHING");
                try(var q=c.prepareStatement("SELECT 1 FROM published_forecast_models WHERE model_id='pollingforecast-2026' AND source_id='pollingforecast-forecasts'")){try(var r=q.executeQuery()){if(!r.next())throw new IllegalStateException("Forecast model source does not match");}}
                run(c,"INSERT INTO polling_forecast_archives(sha256,provider_id,source_url,retrieved_at,payload) VALUES (?,'pollingforecast',?,now(),?) ON CONFLICT(sha256) DO NOTHING",checksum,response.endpoint(),response.payload());
                for(Object value:(List<?>)doc.get("rows")){
                    @SuppressWarnings("unchecked") Map<String,Object> row=(Map<String,Object>)value;String key=(String)row.get("key"),state=(String)row.get("state");
                    if(state==null||!state.matches("[A-Z]{2}"))throw new IllegalArgumentException("Invalid source state");
                    String district=office.equals("HOUSE")&&key.matches(state+"-[0-9]{2}")?key.substring(3):null;
                    if(office.equals("HOUSE")&&district==null)throw new IllegalArgumentException("Invalid provider district");
                    String id="pf-"+kind+"-"+key+"-"+date+"-"+hash(mapper.writeValueAsBytes(row)).substring(0,16);
                    try(var q=c.prepareStatement("INSERT INTO published_forecast_snapshots(forecast_id,model_id,election_year,office,state_code,district_code,race_id,candidate_id,published_at,fetched_at,win_probability,rating,source_date,candidate_name,source_party,source_key,archive_sha256) VALUES (?,'pollingforecast-2026',2026,?,?,?,?,NULL,NULL,now(),?,?,?,?,?,?,?) ON CONFLICT(forecast_id) DO NOTHING")){
                        q.setString(1,id);q.setString(2,office);q.setString(3,state);q.setString(4,district);q.setString(5,"pollingforecast:"+kind+":"+key);q.setObject(6,row.get("chance_left"));q.setString(7,(String)row.get("rating"));q.setObject(8,LocalDate.parse(date));q.setString(9,(String)row.get("left_candidate"));q.setString(10,(String)row.get("left_party"));q.setString(11,key);q.setString(12,checksum);inserted+=q.executeUpdate();
                    }
                }
                c.commit();return inserted;
            }catch(Exception e){c.rollback();throw e;}
        }
    }
}
