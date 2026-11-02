package com.electionsignal;
import static org.junit.jupiter.api.Assertions.*;
import com.electionsignal.polling.PollDeduplicationService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
class PollDeduplicationTests {
  private final PollDeduplicationService service=new PollDeduplicationService();
  private PollDeduplicationService.Survey survey(String population,String matchup,String question,String publication){return new PollDeduplicationService.Survey(" Fictional Research ","Example sponsor","SENATE","TX",null,LocalDate.parse("2026-10-01"),LocalDate.parse("2026-10-04"),population,matchup,question,publication);}
  @Test void syndicatedSurveysRequireSameQuestionAndPopulation(){
    var a=survey("LV","A vs B","ballot test 1","publication-1");
    assertEquals(PollDeduplicationService.Decision.EXACT_MATCH,service.compare(a,survey("LV","A vs B","ballot test 1","publication-1")));
    assertEquals(PollDeduplicationService.Decision.PROBABLE_DUPLICATE,service.compare(a,survey("LV","A vs B","ballot test 1","publication-2")));
    assertEquals(PollDeduplicationService.Decision.DIFFERENT_QUESTIONNAIRE,service.compare(a,survey("RV","A vs B","ballot test 1","publication-1")));
    assertEquals(PollDeduplicationService.Decision.DIFFERENT_QUESTIONNAIRE,service.compare(a,survey("LV","A vs C","ballot test 1","publication-1")));
    assertEquals(PollDeduplicationService.Decision.REVIEW_REQUIRED,service.compare(a,survey(null,"A vs B","ballot test 1","publication-1")));
  }
  @Test void disabledAdapterNeverFetches(){
    var adapter=new com.electionsignal.polling.DisabledPollingAdapter("pollingforecast");
    assertFalse(adapter.enabled());assertEquals("NOT_CONNECTED",adapter.fetchSourceStatus().status());
    assertThrows(IllegalStateException.class,()->adapter.fetchPolls(null));
    assertThrows(IllegalStateException.class,()->adapter.fetchPublishedForecasts(null));
  }
  @Test void missingIdentityRequiresReview(){
    var a=survey("LV","A vs B","ballot test 1","publication-1");
    var noPollster=new PollDeduplicationService.Survey(null,"Example sponsor","SENATE","TX",null,a.start(),a.end(),"LV","A vs B","ballot test 1","other");
    var noDate=new PollDeduplicationService.Survey("Fictional Research","Example sponsor","SENATE","TX",null,null,a.end(),"LV","A vs B","ballot test 1","other");
    var noState=new PollDeduplicationService.Survey("Fictional Research","Example sponsor","SENATE",null,null,a.start(),a.end(),"LV","A vs B","ballot test 1","other");
    assertEquals(PollDeduplicationService.Decision.REVIEW_REQUIRED,service.compare(a,noPollster));
    assertEquals(PollDeduplicationService.Decision.REVIEW_REQUIRED,service.compare(a,noDate));
    assertEquals(PollDeduplicationService.Decision.REVIEW_REQUIRED,service.compare(a,noState));
  }
}
