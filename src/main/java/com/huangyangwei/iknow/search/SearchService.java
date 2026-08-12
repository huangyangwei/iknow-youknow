package com.huangyangwei.iknow.search;
import io.micrometer.core.instrument.*; import java.time.Instant; import java.util.*; import org.springframework.stereotype.Service;
@Service public class SearchService {
 private final Timer timer; public SearchService(MeterRegistry meters){ timer=Timer.builder("knowledge.search.latency").publishPercentileHistogram().register(meters); }
 public SearchResponse search(String query, int limit){ return timer.record(() -> { if(query.isBlank()) throw new IllegalArgumentException("query must not be blank"); return new SearchResponse(query, List.of(), Instant.now()); }); }
 public record SearchResponse(String query, List<SearchHit> items, Instant generatedAt){} public record SearchHit(UUID documentId, String title, String excerpt, double score, long revision){}
}
