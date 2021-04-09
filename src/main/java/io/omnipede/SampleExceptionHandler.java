package io.omnipede;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice
class SampleExceptionHandler {

    @ExceptionHandler(NoHandlerFoundException.class)
    public String noHandlerFoundExceptionHandler(final NoHandlerFoundException e) {

        return e.getMessage();
    }

    @ExceptionHandler(Exception.class)
    public String exceptionHandler(final Exception e) {

        return e.getMessage();
    }
}
