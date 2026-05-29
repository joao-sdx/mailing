package com.synapsedx.mailing.sedia.batch.processor;

import com.synapsedx.mailing.sedia.client.LmStudioClient;
import com.synapsedx.mailing.sedia.model.FundingCall;
import com.synapsedx.mailing.sedia.model.ScoredCall;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RelevanceProcessor implements ItemProcessor<FundingCall, ScoredCall> {

  private final LmStudioClient lmStudioClient;

  @Override
  public ScoredCall process(FundingCall call) {
    var text = buildCallText(call);
    var relevant = lmStudioClient.assessRelevance(text).map(Object::toString).orElse("");
    log.info("call_scored id={} relevant={}", call.identifier(), relevant);
    return new ScoredCall(call, relevant);
  }

  private String buildCallText(FundingCall call) {
    var sb = new StringBuilder();
    sb.append("Titre : ").append(call.title());
    if (call.summary() != null && !call.summary().isBlank()) {
      sb.append("\nDescription : ").append(call.summary());
    }
    return sb.toString();
  }
}
