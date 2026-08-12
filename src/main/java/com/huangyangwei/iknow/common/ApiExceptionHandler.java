package com.huangyangwei.iknow.common;
import java.time.Instant; import java.util.Map; import org.springframework.http.*; import org.springframework.web.bind.MethodArgumentNotValidException; import org.springframework.web.bind.annotation.*;
@RestControllerAdvice public class ApiExceptionHandler {
 @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<ApiError> validation(MethodArgumentNotValidException e){ var f=e.getBindingResult().getFieldErrors().stream().collect(java.util.stream.Collectors.toMap(x->x.getField(),x->x.getDefaultMessage(),(a,b)->a)); return ResponseEntity.badRequest().body(new ApiError("VALIDATION_ERROR","Request validation failed",Instant.now(),f)); }
 @ExceptionHandler(IllegalArgumentException.class) ResponseEntity<ApiError> badRequest(IllegalArgumentException e){return ResponseEntity.badRequest().body(new ApiError("BAD_REQUEST",e.getMessage(),Instant.now(),Map.of()));}
}
