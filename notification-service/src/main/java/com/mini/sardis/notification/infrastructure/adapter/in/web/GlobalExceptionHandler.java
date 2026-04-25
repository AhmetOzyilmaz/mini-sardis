package com.mini.sardis.notification.infrastructure.adapter.in.web;

import com.mini.sardis.common.exception.GlobalExceptionHandlerBase;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler extends GlobalExceptionHandlerBase {
    // All common handlers (validation, generic exception) live in GlobalExceptionHandlerBase.
    // Add notification-service-specific exception handlers here if needed.
}
