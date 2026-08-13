package com.huangyangwei.iknow.rag;

import java.util.*; import org.springframework.jdbc.core.JdbcTemplate; import org.springframework.stereotype.Service;
@Service public class PublishedKnowledgeService { private final JdbcTemplate jdbc; public PublishedKnowledgeService(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public List<ModelAdapter.Evidence> findEvidence(String question){String pattern="%"+question.trim().replace("%","\\%")+"%"; return jdbc.query("select d.id,d.current_revision,r.content from knowledge_document d join knowledge_revision r on r.document_id=d.id and r.revision=d.current_revision where d.status='PUBLISHED' and r.published=true and lower(r.content) like lower(?) order by d.updated_at desc limit 5",(rs,row)->new ModelAdapter.Evidence(rs.getObject(1).toString(),rs.getLong(2),rs.getString(3)),pattern);}
}
