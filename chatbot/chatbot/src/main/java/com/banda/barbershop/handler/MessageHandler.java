package com.banda.barbershop.handler;

import com.banda.barbershop.dto.HandlerRequest;
import com.banda.barbershop.dto.HandlerResponse;
import com.banda.barbershop.enums.ConversationStep;

public interface MessageHandler {
    boolean canHandle(ConversationStep step);

    HandlerResponse handle(HandlerRequest request);

    ConversationStep getHandledStep();
}
