package com.huangyangwei.iknow.ingestion;
import java.time.Instant; import java.util.*; import java.util.concurrent.ConcurrentHashMap; import org.springframework.stereotype.Service;
@Service public class IndexJobService { private final Map<String,Job> jobs=new ConcurrentHashMap<>(); public Job enqueue(UUID doc,long revision,String key){return jobs.computeIfAbsent(key,k->new Job(UUID.randomUUID(),doc,revision,"QUEUED",Instant.now()));} public record Job(UUID id,UUID documentId,long revision,String status,Instant createdAt){} }
