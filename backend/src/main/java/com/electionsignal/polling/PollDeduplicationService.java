package com.electionsignal.polling;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;

/** Conservative reconciliation: ambiguous records are never merged automatically. */
public final class PollDeduplicationService {
    public enum Decision { EXACT_MATCH, PROBABLE_DUPLICATE, DIFFERENT_QUESTIONNAIRE, INDEPENDENT_POLL, REVIEW_REQUIRED }
    public record Survey(String pollster, String sponsor, String office, String state, String district,
                         LocalDate start, LocalDate end, String population, String matchup,
                         String question, String publicationIdentity) { }
    private static String norm(String value) { return value==null?"":value.strip().toLowerCase(Locale.ROOT).replaceAll("\\s+"," "); }
    private static boolean eq(String a,String b) { return norm(a).equals(norm(b)); }
    private static boolean missing(String value) { return norm(value).isEmpty(); }
    public Decision compare(Survey a,Survey b) {
        Objects.requireNonNull(a);Objects.requireNonNull(b);
        if(missing(a.office())||missing(b.office())||missing(a.state())!=missing(b.state()))return Decision.REVIEW_REQUIRED;
        if(!eq(a.office(),b.office())||!eq(a.state(),b.state())||!eq(a.district(),b.district()))return Decision.INDEPENDENT_POLL;
        if(!missing(a.population())&&!missing(b.population())&&!eq(a.population(),b.population()))return Decision.DIFFERENT_QUESTIONNAIRE;
        if(!missing(a.matchup())&&!missing(b.matchup())&&!eq(a.matchup(),b.matchup()))return Decision.DIFFERENT_QUESTIONNAIRE;
        if(!missing(a.question())&&!missing(b.question())&&!eq(a.question(),b.question()))return Decision.DIFFERENT_QUESTIONNAIRE;
        if(!missing(a.pollster())&&!missing(b.pollster())&&!eq(a.pollster(),b.pollster()))return Decision.INDEPENDENT_POLL;
        if(a.start()!=null&&b.start()!=null&&!a.start().equals(b.start())||a.end()!=null&&b.end()!=null&&!a.end().equals(b.end()))return Decision.INDEPENDENT_POLL;
        if(missing(a.pollster())||missing(b.pollster())||a.start()==null||b.start()==null||a.end()==null||b.end()==null||missing(a.population())||missing(b.population())||missing(a.matchup())||missing(b.matchup())||missing(a.question())||missing(b.question())||missing(a.sponsor())!=missing(b.sponsor()))return Decision.REVIEW_REQUIRED;
        if(!eq(a.sponsor(),b.sponsor()))return Decision.REVIEW_REQUIRED;
        if(!missing(a.publicationIdentity())&&!missing(b.publicationIdentity())&&eq(a.publicationIdentity(),b.publicationIdentity()))return Decision.EXACT_MATCH;
        return Decision.PROBABLE_DUPLICATE;
    }
}
