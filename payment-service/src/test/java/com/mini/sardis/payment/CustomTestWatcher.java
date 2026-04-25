package com.mini.sardis.payment;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import java.util.Optional;

@Slf4j
public class CustomTestWatcher implements TestWatcher {

    @Override
    public void testSuccessful(ExtensionContext context) {
        log.info("[PASS] {}", context.getDisplayName());
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        log.error("[FAIL] {} — {}", context.getDisplayName(), cause.getMessage());
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        log.warn("[ABORT] {} — {}", context.getDisplayName(), cause.getMessage());
    }

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        log.info("[SKIP] {} — {}", context.getDisplayName(), reason.orElse("no reason"));
    }
}
