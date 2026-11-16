package com.lifehub.starter.web;

import org.springframework.http.HttpStatus;

/**
 * An error a use case reports to the caller. Rendered as an RFC 9457 problem by {@link ApiExceptionHandler}.
 * The detail is shown to the user, so it must not contain internal information.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    /** Creates an error with the HTTP status and the message shown to the user. */
    public ApiException(HttpStatus status, String detail) {
        super(detail);
        this.status = status;
    }

    /** The HTTP status the error is returned with. */
    public HttpStatus status() {
        return status;
    }

    /** 404: the thing does not exist or the caller may not know it exists. */
    public static ApiException notFound(String detail) {
        return new ApiException(HttpStatus.NOT_FOUND, detail);
    }

    /** 403: the caller is known but not allowed to do this. */
    public static ApiException forbidden(String detail) {
        return new ApiException(HttpStatus.FORBIDDEN, detail);
    }

    /** 409: the request clashes with the current state, for example joining twice. */
    public static ApiException conflict(String detail) {
        return new ApiException(HttpStatus.CONFLICT, detail);
    }

    /** 400: the request is invalid in a way Bean Validation cannot express. */
    public static ApiException badRequest(String detail) {
        return new ApiException(HttpStatus.BAD_REQUEST, detail);
    }
}
