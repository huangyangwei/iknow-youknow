package com.huangyangwei.iknow.conversation;
import com.huangyangwei.iknow.rag.*; import io.micrometer.core.instrument.*; import java.time.Instant; import java.util.*; import java.util.concurrent.*; import org.springframework.stereotype.Service;
@Service public class ConversationService {
 private final ModelAdapter model; private final Timer firstToken; private final Map<UUID,List<Message>> conversations=new ConcurrentHashMap<>();
 public ConversationService(ModelAdapter model,MeterRegistry meters){this.model=model; firstToken=Timer.builder("rag.first_token.latency").publishPercentileHistogram().register(meters);}
 public UUID create(String title){var id=UUID.randomUUID();conversations.put(id,new CopyOnWriteArrayList<>());return id;}
 public List<Message> messages(UUID id){return List.copyOf(conversations.getOrDefault(id,List.of()));}
 public Answer ask(UUID id,String question){if(!conversations.containsKey(id))throw new IllegalArgumentException("conversation not found"); var answer=firstToken.record(()->model.answer(question,List.of())); var m=new Message(UUID.randomUUID(),"assistant",answer.text(),answer.confidence(),List.of(),Instant.now()); conversations.get(id).add(new Message(UUID.randomUUID(),"user",question,null,List.of(),Instant.now())); conversations.get(id).add(m); return new Answer(m);}
 public record Message(UUID id,String role,String content,Double confidence,List<ModelAdapter.Evidence> evidence,Instant createdAt){} public record Answer(Message message){}
}
