package com.electionsignal.polling;

import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.*;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/** Explicitly reviewed, single-survey import. Ambiguous matches abort the transaction. */
@Service @Profile("local")
public class LicensedSurveyImporter {
    private final DataSource db; private final ObjectMapper mapper;
    public LicensedSurveyImporter(DataSource db,ObjectMapper mapper){this.db=db;this.mapper=mapper;}
    private static String required(Map<String,Object> m,String key){Object value=m.get(key);if(!(value instanceof String s)||s.isBlank()||s.length()>500)throw new IllegalArgumentException("Missing or invalid "+key);return s.strip();}
    private static String optional(Map<String,Object> m,String key){Object v=m.get(key);return v==null?null:v.toString().strip();}
    private static String hash(byte[] bytes)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
    private static void exec(Connection c,String sql,Object...args)throws SQLException{try(var q=c.prepareStatement(sql)){for(int i=0;i<args.length;i++)q.setObject(i+1,args[i]);q.execute();}}
    @SuppressWarnings("unchecked")
    public String publish(Path path)throws Exception{
        if(Files.size(path)>10485760)throw new IllegalArgumentException("Input too large");byte[] bytes=Files.readAllBytes(path);var doc=mapper.readValue(bytes,Map.class);
        if(!Boolean.FALSE.equals(doc.get("isTestData")))throw new IllegalArgumentException("Explicit non-test attestation required");
        String source=required(doc,"sourceId"),provider=required(doc,"providerId"),url=required(doc,"publicationUrl"),license=required(doc,"licenseId"),checksum=hash(bytes);
        if(!(doc.get("records") instanceof List<?> records)||records.size()!=1||!(records.get(0) instanceof Map<?,?>))throw new IllegalArgumentException("Exactly one survey required");
        Map<String,Object> x=(Map<String,Object>)records.get(0);if(!"survey".equals(x.get("kind")))throw new IllegalArgumentException("Only surveys are accepted");
        String pollster=required(x,"pollster"),office=required(x,"office"),question=required(x,"questionIdentity"),matchup=required(x,"matchupIdentity"),population=required(x,"population"),external=required(x,"providerPollId"),revision=required(x,"providerRevision");
        String state=optional(x,"state"),district=optional(x,"district"),sponsor=required(x,"sponsor");
        if(!Set.of("HOUSE","SENATE","GOVERNOR","GENERIC").contains(office)||state!=null&&!state.matches("[A-Z]{2}")||district!=null&&(!"HOUSE".equals(office)||state==null||!district.matches("[0-9]{2}")))throw new IllegalArgumentException("Invalid survey scope");
        if(!(x.get("year") instanceof Number y)||y.intValue()<2000||y.intValue()>2100)throw new IllegalArgumentException("Invalid year");
        LocalDate start=LocalDate.parse(required(x,"fieldStart")),end=LocalDate.parse(required(x,"fieldEnd"));if(start.isAfter(end))throw new IllegalArgumentException("Invalid field dates");
        OffsetDateTime published=OffsetDateTime.parse(required(x,"publishedAt"));
        Object sample=x.get("sampleSize"), margin=x.get("marginOfError");
        if(sample!=null&&(!(sample instanceof Number n)||n.longValue()<=0||n.doubleValue()!=n.longValue()||n.longValue()>Integer.MAX_VALUE))throw new IllegalArgumentException("Invalid sample size");
        if(margin!=null&&(!(margin instanceof Number n)||n.doubleValue()<0||n.doubleValue()>100))throw new IllegalArgumentException("Invalid margin of error");
        if(!(x.get("answers") instanceof List<?> answers)||answers.isEmpty()||answers.size()>100)throw new IllegalArgumentException("Survey answers required");
        var choices=new HashSet<String>();for(Object answer:answers){if(!(answer instanceof Map<?,?>))throw new IllegalArgumentException("Invalid answer");var a=(Map<String,Object>)answer;String id=required(a,"choiceId");required(a,"label");if(!choices.add(id))throw new IllegalArgumentException("Duplicate answer");Object p=a.get("percent");if(p!=null&&(!(p instanceof Number n)||n.doubleValue()<0||n.doubleValue()>100))throw new IllegalArgumentException("Invalid share");}
        String pollsterId="pollster-"+hash(pollster.toLowerCase(Locale.ROOT).getBytes(java.nio.charset.StandardCharsets.UTF_8)).substring(0,24);
        var incoming=new PollDeduplicationService.Survey(pollster,sponsor,office,state,district,start,end,population,matchup,question,null);
        try(var c=db.getConnection()){
          c.setAutoCommit(false);try{
            exec(c,"SELECT pg_advisory_xact_lock(373526019)");
            try(var q=c.prepareStatement("SELECT 1 FROM polling_sources WHERE source_id=? AND provider_id=? AND publication_url=? AND license_id=? AND review_status='APPROVED' AND redistribution_allowed=true")){q.setString(1,source);q.setString(2,provider);q.setString(3,url);q.setString(4,license);try(var rs=q.executeQuery()){if(!rs.next())throw new IllegalStateException("Source and individual publication rights must be approved before import");}}
            try(var q=c.prepareStatement("SELECT survey_id,raw_sha256 FROM poll_publications WHERE source_id=? AND provider_poll_id=? AND provider_revision=?")){q.setString(1,source);q.setString(2,external);q.setString(3,revision);try(var rs=q.executeQuery()){if(rs.next()){if(!checksum.equals(rs.getString(2)))throw new IllegalStateException("Revision already exists with different content");c.commit();return "UNCHANGED";}}}
            String match=null;var compare=new PollDeduplicationService();
            try(var q=c.prepareStatement("SELECT s.survey_id,p.display_name,s.sponsor,s.office,s.state_code,s.district_code,s.field_start,s.field_end,s.population,s.matchup_identity,s.question_identity,s.sample_size,s.survey_mode FROM poll_surveys s JOIN pollsters p ON p.pollster_id=s.pollster_id WHERE s.election_year=? AND s.office=? AND s.state_code IS NOT DISTINCT FROM ? AND s.district_code IS NOT DISTINCT FROM ?")){
              q.setInt(1,y.intValue());q.setString(2,office);q.setString(3,state);q.setString(4,district);try(var rs=q.executeQuery()){while(rs.next()){
                var existing=new PollDeduplicationService.Survey(rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5)==null?null:rs.getString(5).trim(),rs.getString(6),rs.getObject(7,LocalDate.class),rs.getObject(8,LocalDate.class),rs.getString(9),rs.getString(10),rs.getString(11),null);
                var decision=compare.compare(incoming,existing);if(decision==PollDeduplicationService.Decision.REVIEW_REQUIRED)throw new IllegalStateException("Ambiguous survey requires review");
                if(decision==PollDeduplicationService.Decision.EXACT_MATCH||decision==PollDeduplicationService.Decision.PROBABLE_DUPLICATE){if((sample==null)!=(rs.getObject(12)==null)||sample!=null&&((Number)sample).intValue()!=rs.getInt(12)||!Objects.equals(optional(x,"surveyMode"),rs.getString(13)))throw new IllegalStateException("Survey method or sample differs; review required");if(match!=null&&!match.equals(rs.getString(1)))throw new IllegalStateException("Multiple survey matches require review");match=rs.getString(1);}
              }}
            }
            if(match!=null){
              var prior=new HashMap<String,String>();try(var q=c.prepareStatement("SELECT a.choice_id,a.choice_label FROM poll_answers a WHERE a.publication_id=(SELECT publication_id FROM poll_publications WHERE survey_id=? ORDER BY publication_id DESC LIMIT 1)")){q.setString(1,match);try(var rs=q.executeQuery()){while(rs.next())prior.put(rs.getString(1),rs.getString(2));}}
              var current=new HashMap<String,String>();for(Object value:answers){var a=(Map<String,Object>)value;current.put(required(a,"choiceId"),required(a,"label"));}
              if(!prior.equals(current))throw new IllegalStateException("Survey answer choices differ; review required");
            }
            if(match==null){match="survey-"+hash((source+"|"+external+"|"+question+"|"+matchup+"|"+population+"|"+start+"|"+end).getBytes(java.nio.charset.StandardCharsets.UTF_8)).substring(0,32);exec(c,"INSERT INTO pollsters(pollster_id,display_name) VALUES (?,?) ON CONFLICT(pollster_id) DO NOTHING",pollsterId,pollster);exec(c,"INSERT INTO poll_surveys(survey_id,pollster_id,sponsor,election_year,office,state_code,district_code,field_start,field_end,population,question_identity,matchup_identity,sample_size,survey_mode,margin_of_error) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",match,pollsterId,sponsor,y.intValue(),office,state,district,start,end,population,question,matchup,sample==null?null:((Number)sample).intValue(),optional(x,"surveyMode"),margin);}
            exec(c,"INSERT INTO poll_raw_archives(sha256,source_id,retrieved_at,payload) VALUES (?,?,now(),?) ON CONFLICT(sha256) DO NOTHING",checksum,source,bytes);
            long publication;try(var q=c.prepareStatement("INSERT INTO poll_publications(survey_id,source_id,provider_poll_id,provider_revision,published_at,fetched_at,raw_sha256) VALUES (?,?,?,?,?,now(),?) RETURNING publication_id")){q.setString(1,match);q.setString(2,source);q.setString(3,external);q.setString(4,revision);q.setObject(5,published);q.setString(6,checksum);try(var rs=q.executeQuery()){rs.next();publication=rs.getLong(1);}}
            for(Object value:answers){var a=(Map<String,Object>)value;exec(c,"INSERT INTO poll_answers(publication_id,choice_id,choice_label,candidate_id,response_percent) VALUES (?,?,?,?,?)",publication,required(a,"choiceId"),required(a,"label"),optional(a,"candidateId"),a.get("percent"));}
            c.commit();return "IMPORTED";
          }catch(Exception e){c.rollback();throw e;}
        }
    }
}
