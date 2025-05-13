package com.example.capstone02.controller;

import com.example.capstone02.util.ConceptKeywordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/concepts")
@RequiredArgsConstructor
@Slf4j
public class ConceptController {

    private final ConceptKeywordMapper conceptKeywordMapper;

    /**
     * 모든 컨셉 정보 조회
     */
    @GetMapping
    public ResponseEntity<List<ConceptDto>> getAllConcepts() {
        List<ConceptDto> concepts = conceptKeywordMapper.getAllConcepts().stream()
                .map(concept -> {
                    List<String> keywords = conceptKeywordMapper.getKeywordsByConcept(concept);
                    return new ConceptDto(concept, keywords);
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(concepts);
    }

    /**
     * 특정 컨셉에 해당하는 키워드 조회
     */
    @GetMapping("/{concept}/keywords")
    public ResponseEntity<List<String>> getKeywordsByConcept(@PathVariable String concept) {
        List<String> keywords = conceptKeywordMapper.getKeywordsByConcept(concept);

        if (keywords.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(keywords);
    }

    /**
     * 키워드로 컨셉 조회
     */
    @GetMapping("/keyword/{keyword}")
    public ResponseEntity<String> getConceptByKeyword(@PathVariable String keyword) {
        String concept = conceptKeywordMapper.getConceptByKeyword(keyword);

        if (concept == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(concept);
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ConceptDto {
        private String name;
        private List<String> keywords;
    }
}