package com.task.aysnctask.services;


import com.task.aysnctask.configurations.RabbitConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class AsyncRabbitProducer {
    private final RabbitTemplate rabbitTemplate;

    public AsyncRabbitProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendToProcessingQueue(Object message) {
        rabbitTemplate.convertAndSend(
                RabbitConfig.EXCHANGE,
                RabbitConfig.PROCESSING_ROUTING_KEY,
                message
        );
    }
}
