package com.huangyangwei.iknow.search;
import jakarta.validation.constraints.*; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/search") public class SearchController { private final SearchService service; public SearchController(SearchService service){this.service=service;} @GetMapping public SearchService.SearchResponse search(@RequestParam @NotBlank String q,@RequestParam(defaultValue="10") @Min(1) @Max(50) int limit){return service.search(q,limit);} }
