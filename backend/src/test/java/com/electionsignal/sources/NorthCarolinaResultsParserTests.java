package com.electionsignal.sources;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import static org.junit.jupiter.api.Assertions.*;

class NorthCarolinaResultsParserTests {
    private static final String HEADER = "County\tElection Date\tPrecinct\tContest Group ID\tContest Type\tContest Name\tChoice\tChoice Party\tVote For\tElection Day\tEarly Voting\tAbsentee by Mail\tProvisional\tTotal Votes\tReal Precinct\n";
    private static final String ROW = "Example\t11/03/2026\tP01\t42\tSTATE\tSenate\tCandidate A\tIND\t1\t10\t0\t\t0\t10\tY\n";
    private byte[] zip(String name, String text, boolean extra) throws Exception {
        var output = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry(name)); zip.write(text.getBytes(StandardCharsets.UTF_8)); zip.closeEntry();
            if (extra) { zip.putNextEntry(new ZipEntry("extra.txt")); zip.write(1); zip.closeEntry(); }
        }
        return output.toByteArray();
    }
    @Test void preservesSourceGeographyAndNullsAndAcceptsCorrections() throws Exception {
        var rows = new ArrayList<NorthCarolinaResultsParser.Row>();
        var parser = new NorthCarolinaResultsParser();
        var summary = parser.parse(zip("results_pct_20261103.txt", HEADER + ROW + ROW.replace("\t10\t0\t\t0\t10\t", "\t7\t0\t\t0\t7\t"), false), rows::add);
        assertEquals(2, summary.rows()); assertEquals(1, summary.counties()); assertEquals(2, summary.nonzeroTotalRows());
        assertEquals("Example", rows.get(0).county()); assertEquals("P01", rows.get(0).precinct());
        assertEquals("IND", rows.get(0).party()); assertNull(rows.get(0).mail()); assertEquals(0L, rows.get(0).early());
        assertEquals(10L, rows.get(0).total()); assertEquals(7L, rows.get(1).total());
    }
    @Test void rejectsIncorrectScopeMalformedVotesAndArchiveEntries() throws Exception {
        var parser = new NorthCarolinaResultsParser();
        assertThrows(IllegalArgumentException.class, () -> parser.parse(zip("../results_pct_20261103.txt", HEADER + ROW, false), row -> {}));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(zip("results_pct_20261103.txt", HEADER + ROW, true), row -> {}));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(zip("results_pct_20261103.txt", HEADER + ROW.replace("2026", "2024"), false), row -> {}));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(zip("results_pct_20261103.txt", HEADER + ROW.replace("\t10\t0\t", "\t-1\t0\t"), false), row -> {}));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(new byte[8 * 1024 * 1024 + 1], row -> {}));
    }
}
