package com.mini.sardis.payment.application.port.in;

public interface ProcessRefundUseCase {
    void execute(ProcessRefundCommand command);
}
