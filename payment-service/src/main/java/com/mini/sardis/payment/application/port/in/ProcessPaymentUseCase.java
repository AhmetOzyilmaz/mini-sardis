package com.mini.sardis.payment.application.port.in;

public interface ProcessPaymentUseCase {
    void execute(ProcessPaymentCommand command);
}
