package com.electionsignal.sources;

/** A provider remains disabled unless a human-recorded rights review permits republication. */
public record SourceRights(String providerId, String licenseId, boolean legalReviewApproved,
                           boolean redistributionAllowed, boolean enabled) {
    public boolean mayPublish() {
        return enabled && legalReviewApproved && redistributionAllowed
                && licenseId != null && !licenseId.isBlank();
    }
}
