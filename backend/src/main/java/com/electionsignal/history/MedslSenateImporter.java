package com.electionsignal.history;

import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/** Import the pinned, public-domain MIT Senate release. No county or House allocation. */
@Service @Profile("local")
public class MedslSenateImporter {
  public static final String SHA256="6f745db1b4a0026ad837e74428f9ed6f3f77fa51eb6f858b58ebdbc186fdb3bd";
  private static final String URL="https://doi.org/10.7910/DVN/PEJ5QU";
  private final DataSource db;
  public MedslSenateImporter(DataSource db){this.db=db;}
  private static String hash(byte[] b)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));}
  private static String[] csv(String line){
    var fields=new ArrayList<String>();var value=new StringBuilder();boolean quoted=false;
    for(int i=0;i<line.length();i++){char ch=line.charAt(i);if(ch=='"'){if(quoted&&i+1<line.length()&&line.charAt(i+1)=='"'){value.append('"');i++;}else quoted=!quoted;}else if(ch==','&&!quoted){fields.add(value.toString());value.setLength(0);}else value.append(ch);}
    if(quoted)throw new IllegalArgumentException("Unclosed CSV field");fields.add(value.toString());return fields.toArray(String[]::new);
  }
  private static void exec(Connection c,String sql,Object...args)throws SQLException{try(var q=c.prepareStatement(sql)){for(int i=0;i<args.length;i++)q.setObject(i+1,args[i]);q.execute();}}
  public int importFile(Path path)throws Exception{
    if(Files.size(path)>2000000)throw new IllegalArgumentException("Unexpected dataset size");byte[] bytes=Files.readAllBytes(path);if(!SHA256.equals(hash(bytes)))throw new IllegalArgumentException("The file does not match the verified MIT V8 checksum");
    String[] lines=new String(bytes,java.nio.charset.StandardCharsets.UTF_8).split("\\R");String[] header=csv(lines[0].replace("\uFEFF",""));var columns=new HashMap<String,Integer>();for(int i=0;i<header.length;i++)columns.put(header[i],i);
    for(String k:List.of("year","state_po","state_fips","office","district","stage","special","candidate","party_detailed","writein","mode","candidatevotes","totalvotes","unofficial"))if(!columns.containsKey(k))throw new IllegalArgumentException("Missing column "+k);
    var rows=new ArrayList<Map<String,String>>();var groups=new LinkedHashMap<String,List<Map<String,String>>>();
    for(int n=1;n<lines.length;n++){
      if(lines[n].isBlank())continue;String[] fields=csv(lines[n]);if(fields.length!=header.length)throw new IllegalArgumentException("Malformed source line "+(n+1));var r=new HashMap<String,String>();for(var e:columns.entrySet())r.put(e.getKey(),fields[e.getValue()]);
      if(!Set.of("2022","2024").contains(r.get("year"))||!"False".equalsIgnoreCase(r.get("unofficial"))||!"GEN".equalsIgnoreCase(r.get("stage")))continue;
      if(!"US SENATE".equalsIgnoreCase(r.get("office"))||!"statewide".equalsIgnoreCase(r.get("district"))||!"TOTAL".equalsIgnoreCase(r.get("mode"))||!r.get("state_po").matches("[A-Z]{2}")||r.get("candidate").isBlank())throw new IllegalArgumentException("Unexpected row scope");
      long votes=new java.math.BigDecimal(r.get("candidatevotes")).longValueExact(), total=new java.math.BigDecimal(r.get("totalvotes")).longValueExact();if(votes<0||total<=0)throw new IllegalArgumentException("Invalid vote totals");
      String key=r.get("year")+"-"+r.get("state_po")+"-"+("True".equalsIgnoreCase(r.get("special"))?"SPECIAL":"REGULAR");groups.computeIfAbsent(key,k->new ArrayList<>()).add(r);rows.add(r);
    }
    if(rows.size()!=302||groups.size()!=65)throw new IllegalArgumentException("Unexpected 2022/2024 release coverage");
    for(var group:groups.values()){long total=new java.math.BigDecimal(group.get(0).get("totalvotes")).longValueExact(),sum=0;for(var r:group){if(new java.math.BigDecimal(r.get("totalvotes")).longValueExact()!=total)throw new IllegalArgumentException("Inconsistent contest total");sum+=new java.math.BigDecimal(r.get("candidatevotes")).longValueExact();}if(sum!=total)throw new IllegalArgumentException("Candidate totals do not reconcile");}
    try(var c=db.getConnection()){c.setAutoCommit(false);try{
      exec(c,"SELECT pg_advisory_xact_lock(373526020)");
      exec(c,"INSERT INTO source_providers(provider_id,display_name,source_url,category,license_id,use_rights_verified,enabled) VALUES ('MEDSL_SENATE_V8','MIT Election Data and Science Lab Senate V8',?,'historical_results','CC0-1.0',true,false) ON CONFLICT(provider_id) DO NOTHING",URL);
      long publication;try(var q=c.prepareStatement("INSERT INTO source_publications(provider_id,title,source_url,citation,license_id,legal_review_status,redistribution_allowed,sha256,content_type,raw_payload_bytes) VALUES ('MEDSL_SENATE_V8','U.S. Senate statewide 1976–2024 V8',?,'MIT Election Data and Science Lab, 2017, U.S. Senate statewide 1976–2024, Harvard Dataverse, V8','CC0-1.0','APPROVED',true,?,'text/csv',?) ON CONFLICT(provider_id,sha256) DO NOTHING RETURNING publication_id")){q.setString(1,URL);q.setString(2,SHA256);q.setBytes(3,bytes);try(var r=q.executeQuery()){if(!r.next()){c.rollback();return 0;}publication=r.getLong(1);}}
      exec(c,"INSERT INTO geography_versions(geography_version_id,authority,source_url,effective_from,legal_review_status,notes) VALUES ('MEDSL_STATE_SCOPE_2022_2024','MIT Election Data and Science Lab',?,'2022-01-01','APPROVED','State identifiers only; no county or district boundary claim') ON CONFLICT(geography_version_id) DO NOTHING",URL);
      for(var e:groups.entrySet()){
        var first=e.getValue().get(0);String id="MEDSL-SEN-"+e.getKey(),state=first.get("state_po"),subtype="True".equalsIgnoreCase(first.get("special"))?"SPECIAL":"REGULAR";int year=Integer.parseInt(first.get("year"));
        exec(c,"INSERT INTO historical_contests(contest_id,election_year,office_code,state_code,seat_class,election_subtype,result_status,publication_id) VALUES (?,?,'SENATE',?,'UNKNOWN',?,'SOURCE_REPORTED',?)",id,year,state,subtype,publication);
        for(var r:e.getValue()){
          String name=r.get("candidate"),party=r.get("party_detailed"),writein=r.get("writein");String candidate="MEDSL-"+hash((name+"|"+party+"|"+writein).getBytes(java.nio.charset.StandardCharsets.UTF_8)).substring(0,24);String stateFips=String.format("%02d",Integer.parseInt(r.get("state_fips")));long votes=new java.math.BigDecimal(r.get("candidatevotes")).longValueExact();
          exec(c,"INSERT INTO historical_result_rows(contest_id,geography_version_id,geo_id,geo_type,state_fips,candidate_id,candidate_name,party_code,vote_method,votes,publication_id) VALUES (?,'MEDSL_STATE_SCOPE_2022_2024',?,'STATE',?,?,?,?, 'TOTAL',?,?)",id,stateFips,stateFips,candidate,name,party,votes,publication);
        }
      }
      c.commit();return rows.size();
    }catch(Exception e){c.rollback();throw e;}}
  }
}
