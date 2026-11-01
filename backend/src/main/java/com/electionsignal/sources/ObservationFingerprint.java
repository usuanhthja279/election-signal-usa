package com.electionsignal.sources;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class ObservationFingerprint {
    private ObservationFingerprint() { }
    public static String sha256(byte[] payload) {
        if (payload == null) throw new IllegalArgumentException("Raw source payload is required");
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload)); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
