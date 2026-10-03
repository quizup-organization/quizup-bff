package io.github.quizup.bff.infrastructure.in.api.problem;

import io.github.quizup.microservice.core.domain.exception.BaseProblem;
import io.github.quizup.microservice.core.domain.exception.ProblemCategory;
import io.github.quizup.microservice.core.infrastructure.in.api.response.ExceptionResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Mappe les {@link BaseProblem} émis par la façade (garde propriétaire, validation de liste) sur
 * le contrat d'erreur RFC 7807 du SDK. Les problèmes levés par les handlers Axon passent par le
 * handler du SDK ({@code Command/QueryExecutionException}) ; un problème levé dans un chaînage
 * asynchrone arrive enveloppé dans un {@code CompletionException}, dont Spring parcourt la chaîne
 * de causes lors de la résolution des {@code @ExceptionHandler}.
 */
@RestControllerAdvice
public class BffProblemExceptionHandler {

    @ExceptionHandler(BaseProblem.class)
    public ResponseEntity<ExceptionResponse> handle(BaseProblem problem, HttpServletRequest request) {
        HttpStatus status = statusFor(problem.getCategory());
        ExceptionResponse response = new ExceptionResponse(
                problem.getType(),
                problem.getCategory(),
                problem.getTitle(),
                problem.getDetail(),
                problem.getContext(),
                status.value(),
                request.getRequestURI());
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(response);
    }

    private static HttpStatus statusFor(ProblemCategory category) {
        return switch (category) {
            case BUSINESS_INVALID_COMMAND, VALIDATION -> HttpStatus.BAD_REQUEST;
            case PERMISSION -> HttpStatus.FORBIDDEN;
            case BUSINESS_RESOURCE_MISSING -> HttpStatus.NOT_FOUND;
            case BUSINESS_AGGREGATE, TECHNICAL -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
