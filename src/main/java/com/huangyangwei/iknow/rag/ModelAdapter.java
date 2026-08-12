package com.huangyangwei.iknow.rag;
import java.util.List;
public interface ModelAdapter { ModelAnswer answer(String question, List<Evidence> evidence); record Evidence(String documentId,long revision,String excerpt){} record ModelAnswer(String text,double confidence){} }
