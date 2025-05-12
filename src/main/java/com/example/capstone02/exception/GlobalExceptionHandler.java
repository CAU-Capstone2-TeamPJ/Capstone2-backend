//package com.example.capstone02.exception;
//
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.bind.annotation.RestControllerAdvice;
//import org.springframework.web.reactive.function.client.WebClientResponseException;
//
//@RestControllerAdvice
//public class GlobalExceptionHandler {
//
//    @ExceptionHandler(WebClientResponseException.class)
//    public ResponseEntity<String> handleWebClientResponseException(WebClientResponseException ex) {
//        return ResponseEntity
//                .status(ex.getStatusCode())
//                .body("외부 API 요청 중 오류가 발생했습니다: " + ex.getMessage());
//    }
//
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<String> handleGeneralException(Exception ex) {
//        return ResponseEntity
//                .status(HttpStatus.INTERNAL_SERVER_ERROR)
//                .body("서버 내부 오류가 발생했습니다: " + ex.getMessage());
//    }
//}