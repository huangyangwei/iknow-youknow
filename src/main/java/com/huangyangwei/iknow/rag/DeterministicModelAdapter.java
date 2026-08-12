package com.huangyangwei.iknow.rag;
import java.util.List; import org.springframework.stereotype.Component;
/** MVP adapter: replace with a Spring AI 2.x ChatModel-backed adapter per deployment profile. */
@Component public class DeterministicModelAdapter implements ModelAdapter { public ModelAnswer answer(String q,List<Evidence> e){ return e.isEmpty()?new ModelAnswer("当前已发布知识中没有足够证据回答该问题。",0):new ModelAnswer(e.getFirst().excerpt(),Math.min(.95,.55+e.size()*.1)); } }
