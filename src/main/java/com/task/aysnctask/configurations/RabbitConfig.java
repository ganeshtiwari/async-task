package com.task.aysnctask.configurations;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String EXCHANGE = "async-exchange";
    public static final String PROCESSING_QUEUE = "processingQueue";

    private static final String RETRY_QUEUE = PROCESSING_QUEUE + ".retry";
    public static final String FAILED_QUEUE = PROCESSING_QUEUE + ".failed";

    public static final String PROCESSING_ROUTING_KEY = "processingRoutingKey";

    @Value("${spring.rabbitmq.retryTtlInMs}")
    private int retryTtlInMs;

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public Queue processingQueue() {
        return QueueBuilder.durable(PROCESSING_QUEUE)
                .deadLetterExchange(EXCHANGE)
                .deadLetterRoutingKey(RETRY_QUEUE)
                .build();
    }

    @Bean
    public Queue retryQueue() {
        return QueueBuilder.durable(RETRY_QUEUE)
                .deadLetterExchange(EXCHANGE)
                .deadLetterRoutingKey(PROCESSING_ROUTING_KEY)
                .ttl(retryTtlInMs)
                .build();
    }

    @Bean
    public Queue failedQueue() {
        return new Queue(FAILED_QUEUE);
    }

    @Bean
    public Binding processingQueueBinding(Queue processingQueue, DirectExchange exchange) {
        return BindingBuilder.bind(processingQueue)
                .to(exchange)
                .with(PROCESSING_ROUTING_KEY);
    }

    @Bean
    public Binding retryQueueBinding(Queue retryQueue, DirectExchange exchange) {
        return BindingBuilder.bind(retryQueue)
                .to(exchange).with(RETRY_QUEUE);
    }

    @Bean
    public Binding failedQueueBinding(Queue failedQueue, DirectExchange exchange) {
        return BindingBuilder.bind(failedQueue)
                .to(exchange)
                .with(FAILED_QUEUE);
    }

}
