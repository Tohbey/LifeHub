package com.lifehub.starter.web;

import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.lifehub.starter.membership.MembershipUnavailableException;

import lombok.extern.slf4j.Slf4j;

/**
 * Renders every error as {@link ProblemDetail}. Spring MVC's own exceptions (validation, malformed JSON,
 * unsupported method) are handled by the superclass.
 */
@RestControllerAdvice
@Slf4j
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    /** Returns an ApiException as a problem with its own status and detail. */
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> handleApi(ApiException ex) {
        return problem(ex.status(), ex.getMessage());
    }

    /** Returns 403 when method security (for example a missing scope) refuses a call. */
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "You do not have permission to do this.");
    }

    /** Returns 400 for a {@code sort} parameter naming a property that does not exist, instead of a 500. */
    @ExceptionHandler(PropertyReferenceException.class)
    ResponseEntity<ProblemDetail> handleUnknownSortProperty(PropertyReferenceException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Unknown sort property.");
    }

    /** Returns 503 when membership could not be checked, so the request is refused (fail closed). */
    @ExceptionHandler(MembershipUnavailableException.class)
    ResponseEntity<ProblemDetail> handleMembershipUnavailable(MembershipUnavailableException ex) {
        log.warn("Denying request because workspace membership could not be checked", ex);
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Access could not be checked right now. Try again shortly.");
    }

    /** Builds a problem response with this status and detail. */
    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
    }
}
