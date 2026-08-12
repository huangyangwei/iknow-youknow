package com.huangyangwei.iknow.audit;
import java.time.Instant; import org.slf4j.*; import org.springframework.stereotype.Service;
@Service public class AuditService { private static final Logger log=LoggerFactory.getLogger(AuditService.class); public void record(String actor,String action,String resource){log.info("audit actor={} action={} resource={} at={}",actor,action,resource,Instant.now());} }
