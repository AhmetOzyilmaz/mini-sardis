package com.mini.sardis.infrastructure.adapter.in.web;

import com.mini.sardis.application.exception.*;
import com.mini.sardis.common.exception.GlobalExceptionHandlerBase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class GlobalExceptionHandler extends GlobalExceptionHandlerBase {

    @ExceptionHandler(SubscriptionNotFoundException.class)
    public ProblemDetail handleSubscriptionNotFound(SubscriptionNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(URI.create(BASE + "subscription-not-found"));
        pd.setTitle("Subscription Not Found");
        return pd;
    }

    @ExceptionHandler(PlanNotFoundException.class)
    public ProblemDetail handlePlanNotFound(PlanNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(URI.create(BASE + "plan-not-found"));
        pd.setTitle("Plan Not Found");
        return pd;
    }

    @ExceptionHandler({InvalidStateTransitionException.class, IllegalStateException.class})
    public ProblemDetail handleInvalidStateTransition(RuntimeException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setType(URI.create(BASE + "invalid-state-transition"));
        pd.setTitle("Invalid State Transition");
        return pd;
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ProblemDetail handleEmailExists(EmailAlreadyExistsException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setType(URI.create(BASE + "email-already-exists"));
        pd.setTitle("Email Already Registered");
        return pd;
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        pd.setType(URI.create(BASE + "invalid-credentials"));
        pd.setTitle("Authentication Failed");
        return pd;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied");
        pd.setType(URI.create(BASE + "access-denied"));
        pd.setTitle("Access Denied");
        return pd;
    }

    @ExceptionHandler(InvalidPromoCodeException.class)
    public ProblemDetail handleInvalidPromoCode(InvalidPromoCodeException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setType(URI.create(BASE + "invalid-promo-code"));
        pd.setTitle("Invalid Promo Code");
        return pd;
    }
}
