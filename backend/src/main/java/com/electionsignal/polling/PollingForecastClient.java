package com.electionsignal.polling;

import java.io.InputStream;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import tools.jackson.databind.ObjectMapper;

/** Explicit, one-shot fetch of the provider's CC-BY forecast output, not its third-party poll rows. */
public final class PollingForecastClient {
    private final HttpClient http;
    private final ObjectMapper mapper;
    public PollingForecastClient(ObjectMapper mapper) { this.mapper=mapper; this.http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NEVER).build(); }
    public record Response(String endpoint, byte[] payload, Map<String,Object> document) {}
    @SuppressWarnings("unchecked")
    public Response fetch(String kind) throws Exception {
        if (!Set.of("senate","governors","house").contains(kind)) throw new IllegalArgumentException("Unsupported forecast dataset");
        String endpoint="https://www.pollingforecast.com/api/us/"+kind;
        HttpRequest request=HttpRequest.newBuilder(URI.create(endpoint)).timeout(Duration.ofSeconds(20)).header("Accept","application/json").header("User-Agent","ElectionSignal/1.0 (manual forecast import)").GET().build();
        HttpResponse<InputStream> response=http.send(request,HttpResponse.BodyHandlers.ofInputStream());
        try(InputStream input=response.body()) {
            if(response.statusCode()!=200) throw new IllegalStateException("Forecast provider HTTP "+response.statusCode()+"; retry after "+response.headers().firstValue("Retry-After").orElse("unspecified"));
            byte[] bytes=input.readNBytes(5242881);
            if(bytes.length>5242880) throw new IllegalStateException("Provider payload too large");
            Map<String,Object> doc=mapper.readValue(bytes,Map.class);
            validate(kind,doc);
            return new Response(endpoint,bytes,doc);
        }
    }
    public static void validate(String kind,Map<String,Object> doc) {
        if(!kind.equals(doc.get("dataset")) || !"CC BY 4.0".equals(doc.get("licence")) || !(doc.get("rows") instanceof List<?> rows) || !(doc.get("count") instanceof Number count) || count.intValue()!=rows.size()) throw new IllegalArgumentException("Unexpected dataset, license, or row count");
        if(!(doc.get("description") instanceof String description) || !description.contains("2026")) throw new IllegalArgumentException("The provider did not identify the 2026 cycle");
        if(!(doc.get("asOf") instanceof String asOf)) throw new IllegalArgumentException("Missing source date");
        java.time.LocalDate.parse(asOf);
        for(Object value:rows) {
            if(!(value instanceof Map<?,?> row) || !"CC BY 4.0".equals(row.get("licence")) || !(row.get("key") instanceof String key) || !key.matches("(?:[A-Z]{2}(?:-[A-Z0-9]{1,3})?|G-[A-Z]{2})")) throw new IllegalArgumentException("Unlicensed or unidentifiable forecast row");
            if(!(row.get("state") instanceof String state) || !state.matches("[A-Z]{2}") || !(kind.equals("governors") ? key.equals("G-"+state) : kind.equals("house") ? key.matches(state+"-[0-9]{2}") : key.equals(state)||key.equals(state+"-S"))) throw new IllegalArgumentException("Forecast key does not match its state and office");
            if(row.get("chance_left")!=null && (!(row.get("chance_left") instanceof Number n) || n.doubleValue()<0 || n.doubleValue()>1)) throw new IllegalArgumentException("Invalid publisher probability");
        }
    }
}

