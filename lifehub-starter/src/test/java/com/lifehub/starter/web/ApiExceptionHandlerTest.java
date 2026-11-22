package com.lifehub.starter.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;

class ApiExceptionHandlerTest {

    record Body(String name, Integer amount) {
    }

    @Test
    void validationErrorsNameEachInvalidField() throws Exception {
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(new Body("", -1), "body");
        result.addError(new FieldError("body", "name", "", false, null, null, "must not be blank"));
        result.addError(new FieldError("body", "amount", -1, false, null, null, "must be greater than 0"));
        MethodParameter parameter = new MethodParameter(
                ApiExceptionHandlerTest.class.getDeclaredMethod("validationErrorsNameEachInvalidField"), -1);

        ResponseEntity<Object> response = new ApiExceptionHandler().handleMethodArgumentNotValid(
                new MethodArgumentNotValidException(parameter, result), new HttpHeaders(), HttpStatus.BAD_REQUEST,
                new ServletWebRequest(new MockHttpServletRequest()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail problem = (ProblemDetail) response.getBody();
        assertThat(problem.getDetail()).isEqualTo("Some fields are invalid.");
        assertThat(problem.getProperties()).containsEntry("errors",
                Map.of("name", "must not be blank", "amount", "must be greater than 0"));
        assertThat(problem.toString()).doesNotContain("-1");
    }
}
