package com.electionsignal.sources;

import java.net.URI;
import java.net.HttpURLConnection;

/** Explicit read-only preflight: no scheduler, no persistence, no public results. */
public final class NorthCarolinaResultsPreflight {
    public static final URI URL = URI.create("https://dl.ncsbe.gov/ENRS/2026_11_03/results_pct_20261103.zip");
    public NorthCarolinaResultsParser.Summary fetch() throws Exception {
        return new NorthCarolinaResultsParser().parse(fetchBytes(), row -> {});
    }
    public byte[] fetchBytes() throws Exception {
        var connection = (HttpURLConnection) URL.toURL().openConnection();
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(40000);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("Accept", "application/zip, application/octet-stream");
        connection.setRequestProperty("User-Agent", "ElectionSignal/1.0 (manual source preflight)");
        try {
            int status = connection.getResponseCode();
            if (status != 200) throw new IllegalStateException("North Carolina results HTTP " + status);
            try (var body = connection.getInputStream()) {
                byte[] bytes = body.readNBytes(8 * 1024 * 1024 + 1);
                if (bytes.length > 8 * 1024 * 1024) throw new IllegalArgumentException("Source response exceeds 8 MiB");
                return bytes;
            }
        } finally { connection.disconnect(); }
    }
}
