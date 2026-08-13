package com.huangyangwei.iknow.ingestion;

import com.huangyangwei.iknow.common.ConflictException; import java.sql.ResultSet; import java.time.Instant; import java.util.UUID; import org.springframework.jdbc.core.JdbcTemplate; import org.springframework.stereotype.Service;
@Service public class IndexJobService {
 private final JdbcTemplate jdbc; public IndexJobService(JdbcTemplate jdbc){this.jdbc=jdbc;}
 /** Called within the revision transaction: reservation and state switch either commit or roll back together. */
 public Reservation reserve(UUID documentId,long revision,Operation operation,String key){
  var job=new Job(UUID.randomUUID(),documentId,revision,operation,"QUEUED",Instant.now()); int inserted=jdbc.update("insert into job_task(id,task_type,operation,status,document_id,target_revision,idempotency_key,created_at,updated_at) values(?,?,?,?,?,?,?,?,?) on conflict (document_id,target_revision,operation,idempotency_key) do nothing",job.id(),"INDEX",operation.name(),job.status(),documentId,revision,key,job.createdAt(),job.createdAt());
  return inserted==1?new Reservation(job,true):new Reservation(find(documentId,revision,operation,key),false);
 }
 private Job find(UUID documentId,long revision,Operation operation,String key){return jdbc.query("select id,document_id,target_revision,operation,status,created_at from job_task where document_id=? and target_revision=? and operation=? and idempotency_key=?",rs->{if(!rs.next())throw new ConflictException("idempotency reservation was not found");return map(rs);},documentId,revision,operation.name(),key);}
 private Job map(ResultSet rs)throws java.sql.SQLException{return new Job(rs.getObject("id",UUID.class),rs.getObject("document_id",UUID.class),rs.getLong("target_revision"),Operation.valueOf(rs.getString("operation")),rs.getString("status"),rs.getTimestamp("created_at").toInstant());}
 public enum Operation { PUBLISH, ROLLBACK } public record Job(UUID id,UUID documentId,long revision,Operation operation,String status,Instant createdAt){} public record Reservation(Job job,boolean created){}
}
