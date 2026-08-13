package com.huangyangwei.iknow.ingestion;

import com.huangyangwei.iknow.common.ConflictException;
import java.sql.ResultSet; import java.time.Instant; import java.util.UUID;
import org.springframework.dao.DuplicateKeyException; import org.springframework.jdbc.core.JdbcTemplate; import org.springframework.stereotype.Service;

@Service public class IndexJobService {
 private final JdbcTemplate jdbc; public IndexJobService(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public Job enqueue(UUID documentId,long revision,Operation operation,String key){
  try { var job=new Job(UUID.randomUUID(),documentId,revision,operation,"QUEUED",Instant.now()); jdbc.update("insert into job_task(id,task_type,operation,status,document_id,target_revision,idempotency_key,created_at,updated_at) values(?,?,?,?,?,?,?,?,?)",job.id(),"INDEX",operation.name(),job.status(),documentId,revision,key,job.createdAt(),job.createdAt()); return job;
  } catch(DuplicateKeyException duplicate) { return jdbc.query("select id,document_id,target_revision,operation,status,created_at from job_task where document_id=? and target_revision=? and operation=? and idempotency_key=?",rs->{if(!rs.next())throw new ConflictException("conflicting idempotency request");return map(rs);},documentId,revision,operation.name(),key); }
 }
 private Job map(ResultSet rs)throws java.sql.SQLException{return new Job(rs.getObject("id",UUID.class),rs.getObject("document_id",UUID.class),rs.getLong("target_revision"),Operation.valueOf(rs.getString("operation")),rs.getString("status"),rs.getTimestamp("created_at").toInstant());}
 public enum Operation { PUBLISH, ROLLBACK }
 public record Job(UUID id,UUID documentId,long revision,Operation operation,String status,Instant createdAt){}
}
