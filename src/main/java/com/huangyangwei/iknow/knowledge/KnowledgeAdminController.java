package com.huangyangwei.iknow.knowledge;

import com.huangyangwei.iknow.common.ConflictException; import com.huangyangwei.iknow.ingestion.IndexJobService; import jakarta.validation.Valid; import jakarta.validation.constraints.*; import java.util.*; import org.springframework.http.*; import org.springframework.jdbc.core.JdbcTemplate; import org.springframework.transaction.annotation.Transactional; import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/admin/knowledge") public class KnowledgeAdminController {
 private final IndexJobService jobs; private final JdbcTemplate jdbc; public KnowledgeAdminController(IndexJobService jobs,JdbcTemplate jdbc){this.jobs=jobs;this.jdbc=jdbc;}
 @PostMapping("/{id}/revisions/{revision}:publish") @Transactional public ResponseEntity<IndexJobService.Job> publish(@PathVariable UUID id,@PathVariable @Positive long revision,@RequestHeader("Idempotency-Key") @NotBlank String key,@RequestBody(required=false) @Valid PublishRequest request){return ResponseEntity.accepted().body(switchRevision(id,revision,key,IndexJobService.Operation.PUBLISH,request==null?null:request.expectedVersion()));}
 @PostMapping("/{id}/revisions/{revision}:rollback") @Transactional public ResponseEntity<IndexJobService.Job> rollback(@PathVariable UUID id,@PathVariable @Positive long revision,@RequestHeader("Idempotency-Key") @NotBlank String key,@RequestBody(required=false) @Valid PublishRequest request){return ResponseEntity.accepted().body(switchRevision(id,revision,key,IndexJobService.Operation.ROLLBACK,request==null?null:request.expectedVersion()));}
 private IndexJobService.Job switchRevision(UUID id,long revision,String key,IndexJobService.Operation operation,Long expectedVersion){
  var reservation=jobs.reserve(id,revision,operation,key); if(!reservation.created()) return reservation.job();
  var version=jdbc.query("select version from knowledge_document where id=? for update",rs->rs.next()?rs.getLong(1):null,id); if(version==null)throw new IllegalArgumentException("knowledge document not found"); if(expectedVersion!=null&&!expectedVersion.equals(version))throw new ConflictException("knowledge document version conflict");
  var exists=jdbc.queryForObject("select count(*) from knowledge_revision where document_id=? and revision=?",Integer.class,id,revision); if(exists==null||exists==0)throw new IllegalArgumentException("knowledge revision not found");
  jdbc.update("update knowledge_revision set published=false where document_id=?",id); jdbc.update("update knowledge_revision set published=true where document_id=? and revision=?",id,revision);
  if(jdbc.update("update knowledge_document set current_revision=?, status=?, version=version+1, updated_at=current_timestamp where id=? and version=?",revision,"PUBLISHED",id,version)!=1)throw new ConflictException("knowledge document version conflict");
  return reservation.job();
 }
 public record PublishRequest(@PositiveOrZero Long expectedVersion){}
}
