package com.electionsignal.sources;

import java.io.InputStream;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;
import tools.jackson.databind.ObjectMapper;

/** Manual, read-only preflight for the official CA 2026 Governor feed; never publishes votes. */
public final class CaliforniaResultsPreflight {
  public static final URI URL=URI.create("https://media.sos.ca.gov/media/governor.json");
  private final ObjectMapper mapper;
  public CaliforniaResultsPreflight(ObjectMapper mapper){this.mapper=mapper;}
  public record Status(String availability,String sourceTimeText,Integer precinctPercent,int candidateCount){}
  public Status fetch()throws Exception{
    var client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NEVER).build();
    var request=HttpRequest.newBuilder(URL).timeout(Duration.ofSeconds(20)).header("Accept","application/json").header("User-Agent","ElectionSignal/1.0 (manual source preflight)").GET().build();
    var response=client.send(request,HttpResponse.BodyHandlers.ofInputStream());
    try(InputStream input=response.body()){if(response.statusCode()!=200)throw new IllegalStateException("California results HTTP "+response.statusCode());byte[] body=input.readNBytes(1048577);if(body.length>1048576)throw new IllegalStateException("California results response too large");return inspect(mapper.readValue(body,List.class));}
  }
  public static Status inspect(List<?> entries){
    var regex=Pattern.compile("^(\\d{1,3})% \\(([0-9,]+) of ([0-9,]+)\\) precincts reporting$",Pattern.CASE_INSENSITIVE);
    for(Object entry:entries){if(!(entry instanceof Map<?,?> r))throw new IllegalArgumentException("Invalid response row");if(!"Governor - Statewide Results".equals(r.get("raceTitle")))continue;
      if(!(r.get("Reporting") instanceof String text)||!(r.get("ReportingTime") instanceof String time)||!(r.get("candidates") instanceof List<?> candidates)||candidates.isEmpty())throw new IllegalArgumentException("Incomplete statewide response");
      var m=regex.matcher(text);if(!m.matches())throw new IllegalArgumentException("Unsupported precinct reporting format");int percent=Integer.parseInt(m.group(1));long reported=Long.parseLong(m.group(2).replace(",","")),total=Long.parseLong(m.group(3).replace(",",""));if(percent>100||total<=0||reported>total)throw new IllegalArgumentException("Invalid precinct reporting");
      boolean votes=false;for(Object candidate:candidates){if(!(candidate instanceof Map<?,?> c)||!(c.get("Name") instanceof String name)||name.isBlank()||!(c.get("Votes") instanceof String count)||!count.matches("[0-9,]+"))throw new IllegalArgumentException("Invalid candidate record");if(Long.parseLong(count.replace(",",""))>0)votes=true;}
      return new Status(!votes&&reported==0?"NOT_REPORTING":"SOURCE_UNVERIFIED",time,percent,candidates.size());
    }throw new IllegalArgumentException("Statewide Governor record absent");
  }
}
