package com.electionsignal.history;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/** Pinned, separately attributed Election Data distribution of MEDSL House V15. */
@Service @Profile("local")
public class HouseResultsImporter {
  public static final String SHA256="38db9d28dc4d3e039de97c89aedb921ca5f57dd137baf527c937b76bc8760214";
  private static final String URL="https://api.electiondata.dev/v1/house-results@2026-10-26.cc2e247a45/download/house-results.csv";
  private final DataSource db;
  public HouseResultsImporter(DataSource db){this.db=db;}
  private static String hash(byte[] b)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));}
  private static String[] csv(String line){
    var fields=new ArrayList<String>();var value=new StringBuilder();boolean quoted=false;
    for(int i=0;i<line.length();i++){char ch=line.charAt(i);if(ch=='"'){if(quoted&&i+1<line.length()&&line.charAt(i+1)=='"'){value.append('"');i++;}else quoted=!quoted;}else if(ch==','&&!quoted){fields.add(value.toString());value.setLength(0);}else value.append(ch);}
    if(quoted)throw new IllegalArgumentException("Unclosed CSV field");fields.add(value.toString());return fields.toArray(String[]::new);
  }
  private static void exec(Connection c,String sql,Object...args)throws SQLException{try(var q=c.prepareStatement(sql)){for(int i=0;i<args.length;i++)q.setObject(i+1,args[i]);q.execute();}}
  public int importFile(Path path)throws Exception{
    if(Files.size(path)>6_000_000)throw new IllegalArgumentException("Unexpected source file size");byte[] bytes=Files.readAllBytes(path);
    if(!SHA256.equals(hash(bytes)))throw new IllegalArgumentException("Source does not match pinned Election Data distribution");
    String[] lines=new String(bytes,StandardCharsets.UTF_8).split("\\R");String[] header=csv(lines[0].replace("\uFEFF",""));var columns=new HashMap<String,Integer>();for(int i=0;i<header.length;i++)columns.put(header[i],i);
    for(String k:List.of("year","state_po","state_fips","office","district","stage","runoff","special","race_row","candidate","party","writein","fusion_ticket","mode","candidatevotes","totalvotes","unofficial"))if(!columns.containsKey(k))throw new IllegalArgumentException("Missing column "+k);
    var groups=new LinkedHashMap<String,List<Map<String,String>>>();int count=0;
    for(int n=1;n<lines.length;n++){
      if(lines[n].isBlank())continue;String[] fields=csv(lines[n]);if(fields.length!=header.length)throw new IllegalArgumentException("Malformed line "+(n+1));var r=new HashMap<String,String>();for(var e:columns.entrySet())r.put(e.getKey(),fields[e.getValue()]);
      if(!Set.of("2022","2024").contains(r.get("year"))||!"GEN".equals(r.get("stage"))||!"false".equals(r.get("special"))||!"false".equals(r.get("runoff"))||!"false".equals(r.get("unofficial"))||"DC".equals(r.get("state_po"))||Set.of("BLANK","OVERVOTES","UNDERVOTES").contains(r.get("candidate")))continue;
      if(!"US HOUSE".equals(r.get("office"))||!"TOTAL".equals(r.get("mode"))||!r.get("state_po").matches("[A-Z]{2}")||!r.get("state_fips").matches("[0-9]{1,2}")||!r.get("district").matches("[0-9]{1,2}")||!r.get("race_row").matches("[0-9]{1,4}")||r.get("candidate").isBlank()||(!r.get("candidatevotes").isBlank()&&!r.get("candidatevotes").matches("[0-9]{1,12}")))throw new IllegalArgumentException("Invalid House row scope");
      String key=r.get("year")+"-"+r.get("state_po")+"-"+String.format("%02d",Integer.parseInt(r.get("district")));groups.computeIfAbsent(key,k->new ArrayList<>()).add(r);count++;
    }
    if(count!=2628||groups.size()!=870)throw new IllegalArgumentException("Unexpected pinned House coverage");
    for(var group:groups.values()){var ids=new HashSet<String>();for(var r:group)if(!ids.add(r.get("race_row")))throw new IllegalArgumentException("Duplicate ballot line");}
    try(var c=db.getConnection()){c.setAutoCommit(false);try{
      exec(c,"SELECT pg_advisory_xact_lock(373526021)");
      exec(c,"INSERT INTO source_providers(provider_id,display_name,source_url,category,license_id,use_rights_verified,enabled) VALUES ('ELECTIONDATA_HOUSE_V15','Election Data distribution of MIT House V15',?,'historical_results','CC0-1.0',true,false) ON CONFLICT(provider_id) DO NOTHING",URL);
      long publication;try(var q=c.prepareStatement("INSERT INTO source_publications(provider_id,title,source_url,citation,license_id,legal_review_status,redistribution_allowed,sha256,content_type,raw_payload_bytes) VALUES ('ELECTIONDATA_HOUSE_V15','MIT House V15 via Election Data 2026-10-26',?,'MIT Election Data and Science Lab, 2017, U.S. House 1976–2024, Harvard Dataverse V15; distributed by electiondata.dev','CC0-1.0','APPROVED',true,?,'text/csv',?) ON CONFLICT(provider_id,sha256) DO NOTHING RETURNING publication_id")){q.setString(1,URL);q.setString(2,SHA256);q.setBytes(3,bytes);try(var r=q.executeQuery()){if(!r.next()){c.rollback();return 0;}publication=r.getLong(1);}}
      exec(c,"INSERT INTO geography_versions(geography_version_id,authority,source_url,effective_from,legal_review_status,notes) VALUES ('MEDSL_HOUSE_DISTRICT_IDENTIFIERS_2022_2024','MIT Election Data and Science Lab',?,'2022-01-01','APPROVED','Source election-year district identifiers only; no polygon or 2026 boundary claim') ON CONFLICT(geography_version_id) DO NOTHING",URL);
      for(var e:groups.entrySet()){
        var first=e.getValue().get(0);String id="MEDSL-HOUSE-"+e.getKey(),state=first.get("state_po"),district=String.format("%02d",Integer.parseInt(first.get("district")));int year=Integer.parseInt(first.get("year"));
        exec(c,"INSERT INTO historical_contests(contest_id,election_year,office_code,state_code,district_code,election_subtype,result_status,publication_id) VALUES (?,?,'HOUSE',?,?,'REGULAR','SOURCE_REPORTED',?)",id,year,state,district,publication);
        for(var r:e.getValue()){
          String fips=String.format("%02d",Integer.parseInt(r.get("state_fips"))),candidate="MEDSL-HOUSE-LINE-"+hash((id+"|"+r.get("race_row")).getBytes(StandardCharsets.UTF_8)).substring(0,24);
          Long votes=r.get("candidatevotes").isBlank()?null:Long.valueOf(r.get("candidatevotes"));
          exec(c,"INSERT INTO historical_result_rows(contest_id,geography_version_id,geo_id,geo_type,state_fips,candidate_id,candidate_name,party_code,vote_method,votes,publication_id) VALUES (?,'MEDSL_HOUSE_DISTRICT_IDENTIFIERS_2022_2024',?,'DISTRICT',?,?,?,?, 'BALLOT_LINE',?,?)",id,fips+district,fips,candidate,r.get("candidate"),r.get("party").isBlank()?null:r.get("party"),votes,publication);
        }
      }
      c.commit();return count;
    }catch(Exception e){c.rollback();throw e;}}
  }
}
