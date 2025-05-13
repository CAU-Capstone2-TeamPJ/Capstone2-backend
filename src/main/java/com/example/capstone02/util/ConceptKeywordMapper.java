package com.example.capstone02.util;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 컨셉과 추천 키워드 간의 매핑을 관리하는 유틸리티 클래스
 */
@Component
@Getter
public class ConceptKeywordMapper {

    // 컨셉에 해당하는 키워드 매핑
    private final Map<String, List<String>> conceptToKeywordsMap;

    // 키워드가 속한 컨셉 매핑 (역방향 매핑)
    private final Map<String, String> keywordToConceptMap;

    public ConceptKeywordMapper() {
        conceptToKeywordsMap = new HashMap<>();
        keywordToConceptMap = new HashMap<>();

        // 컨셉별 키워드 매핑 초기화
        conceptToKeywordsMap.put("맛으로 기억되는 순간", Arrays.asList("식당", "카페", "먹방"));
        conceptToKeywordsMap.put("몸으로 느끼는 즐거움", Arrays.asList("액티비티", "공포"));
        conceptToKeywordsMap.put("마음이 쉬어가는 곳", Arrays.asList("관광", "포토스팟", "힐링", "자연"));
        conceptToKeywordsMap.put("시간이 담긴 이야기", Arrays.asList("문화", "예술", "역사"));
        conceptToKeywordsMap.put("손에 담는 기쁨", Collections.singletonList("쇼핑"));

        // 역방향 매핑 초기화 (키워드 -> 컨셉)
        for (Map.Entry<String, List<String>> entry : conceptToKeywordsMap.entrySet()) {
            String concept = entry.getKey();
            List<String> keywords = entry.getValue();

            for (String keyword : keywords) {
                keywordToConceptMap.put(keyword, concept);
            }
        }
    }

    /**
     * 컨셉에 해당하는 키워드 목록 반환
     */
    public List<String> getKeywordsByConcept(String concept) {
        return conceptToKeywordsMap.getOrDefault(concept, Collections.emptyList());
    }

    /**
     * 키워드가 속한 컨셉 반환
     */
    public String getConceptByKeyword(String keyword) {
        return keywordToConceptMap.getOrDefault(keyword, null);
    }

    /**
     * 키워드 목록에서 컨셉에 해당하는 키워드만 필터링하여 반환
     */
    public List<String> filterKeywordsByConcept(List<String> keywords, String concept) {
        if (keywords == null || concept == null) {
            return Collections.emptyList();
        }

        List<String> conceptKeywords = getKeywordsByConcept(concept);
        return keywords.stream()
                .filter(conceptKeywords::contains)
                .collect(Collectors.toList());
    }

    /**
     * 키워드 목록에서 관련된 컨셉 목록 반환
     */
    public Set<String> getConceptsFromKeywords(List<String> keywords) {
        if (keywords == null) {
            return Collections.emptySet();
        }

        return keywords.stream()
                .map(this::getConceptByKeyword)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * 모든 컨셉 목록 반환
     */
    public List<String> getAllConcepts() {
        return new ArrayList<>(conceptToKeywordsMap.keySet());
    }

    /**
     * 모든 키워드 목록 반환
     */
    public List<String> getAllKeywords() {
        return new ArrayList<>(keywordToConceptMap.keySet());
    }
}