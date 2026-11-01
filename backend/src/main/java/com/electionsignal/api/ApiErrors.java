package com.electionsignal.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ProblemDetail> status(ResponseStatusException error) {
        String code = error.getStatusCode().value() == 404 ? "NOT_FOUND" : error.getStatusCode().value() == 503 ? "SOURCE_UNAVAILABLE" : "INVALID_PARAMETER";
        ProblemDetail detail=ProblemDetail.forStatusAndDetail(error.getStatusCode(),error.getReason()==null?code:error.getReason());
        detail.setTitle(code);detail.setProperty("error",code);
        return ResponseEntity.status(error.getStatusCode()).body(detail);
    }
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ProblemDetail> invalidArgument(MethodArgumentTypeMismatchException error) {
        ProblemDetail detail=ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,"Invalid " + error.getName());
        detail.setTitle("INVALID_PARAMETER");detail.setProperty("error","INVALID_PARAMETER");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(detail);
    }
}
