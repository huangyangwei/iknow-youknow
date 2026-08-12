package com.huangyangwei.iknow.conversation;
import jakarta.validation.Valid; import jakarta.validation.constraints.*; import java.util.*; import org.springframework.http.MediaType; import org.springframework.web.bind.annotation.*; import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
@RestController @RequestMapping("/api/v1/conversations") public class ConversationController { private final ConversationService service; public ConversationController(ConversationService s){service=s;}
 @PostMapping public Map<String,UUID> create(@RequestBody @Valid CreateRequest r){return Map.of("id",service.create(r.title()));} @GetMapping("/{id}/messages") public List<ConversationService.Message> messages(@PathVariable UUID id){return service.messages(id);}
 @PostMapping("/{id}/questions") public ConversationService.Answer ask(@PathVariable UUID id,@RequestBody @Valid QuestionRequest r){return service.ask(id,r.question());}
 @PostMapping(value="/{id}/questions:stream",produces=MediaType.TEXT_EVENT_STREAM_VALUE) public SseEmitter stream(@PathVariable UUID id,@RequestBody @Valid QuestionRequest r){var emitter=new SseEmitter(5000L); try { var answer=service.ask(id,r.question()); emitter.send(SseEmitter.event().name("answer").data(answer)); emitter.send(SseEmitter.event().name("done").data(Map.of("status","complete"))); emitter.complete(); }catch(Exception e){emitter.completeWithError(e);} return emitter; }
 public record CreateRequest(@Size(max=255) String title){} public record QuestionRequest(@NotBlank @Size(max=4000) String question){}
}
