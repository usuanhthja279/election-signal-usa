package com.electionsignal.history;

import java.util.HashSet;
import java.util.Set;

/** Strict structural checks run before reviewed source files can be staged or published. */
public final class HistoricalRowValidator {
    private HistoricalRowValidator() { }
    public record Row(String contestId, String office, String candidateId, String geoType, String geoId,
                      String stateFips, String countyFips, String districtCode, String geographyVersion,
                      String voteMethod, String certificationStatus, Long votes) { }
    public static Set<String> validate(Iterable<Row> rows) {
        var keys = new HashSet<String>();
        for (Row row : rows) {
            require(row.contestId(), "contestId"); require(row.office(), "office");
            require(row.candidateId(), "candidateId"); require(row.geoId(), "geoId");
            require(row.geoType(), "geoType"); require(row.geographyVersion(), "geographyVersion");
            require(row.voteMethod(), "voteMethod");
            if (!Set.of("HOUSE", "SENATE", "GOVERNOR").contains(row.office())) fail("unsupported office");
            if (!Set.of("STATE", "COUNTY", "PRECINCT", "DISTRICT").contains(row.geoType())) fail("unsupported geography type");
            if (!"CERTIFIED".equals(row.certificationStatus())) fail("only certified observations may be imported");
            if (row.stateFips() != null && !row.stateFips().matches("[0-9]{2}")) fail("state FIPS must be two digits");
            if (row.countyFips() != null && !row.countyFips().matches("[0-9]{5}")) fail("county FIPS must be five digits");
            if (row.office().equals("HOUSE") && (!row.geoType().equals("DISTRICT") || row.districtCode() == null || !row.districtCode().matches("[0-9]{2}")))
                fail("House results require an exact two-digit district geography; county or precinct totals need a verified crosswalk");
            if (row.votes() != null && row.votes() < 0) fail("negative totals are not valid imported results");
            String key = String.join("|", row.contestId(), row.candidateId(), row.geoType(), row.geoId(), row.geographyVersion(), row.voteMethod());
            if (!keys.add(key)) fail("duplicate contest/candidate/geography/method row");
        }
        return Set.copyOf(keys);
    }
    private static void require(String value,String name){if(value==null||value.isBlank())fail(name+" is required");}
    private static void fail(String reason){throw new IllegalArgumentException(reason);}
}
