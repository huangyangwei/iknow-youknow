package com.huangyangwei.iknow.knowledge;
import com.huangyangwei.iknow.ingestion.*; import jakarta.validation.Valid; import jakarta.validation.constraints.*; import java.util.*; import org.springframework.http.*; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/admin/knowledge") public class KnowledgeAdminController { private final IndexJobService jobs; public KnowledgeAdminController(IndexJobService jobs){this.jobs=jobs;}
 @PostMapping("/{id}/revisions/{revision}:publish") public ResponseEntity<IndexJobService.Job> publish(@PathVariable UUID id,@PathVariable @Positive long revision,@RequestHeader("Idempotency-Key") @NotBlank String key,@RequestBody(required=false) @Valid PublishRequest request){return ResponseEntity.accepted().body(jobs.enqueue(id,revision,key));}
 @PostMapping("/{id}/revisions/{revision}:rollback") public Map<String,Object> rollback(@PathVariable UUID id,@PathVariable @Positive long revision){return Map.of("documentId",id,"activeRevision",revision,"atomic",true);}
 public record PublishRequest(@PositiveOrZero Long expectedVersion){}
}
