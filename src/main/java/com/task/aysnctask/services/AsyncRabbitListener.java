package com.task.aysnctask.services;

import com.task.aysnctask.configurations.RabbitConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class AsyncRabbitListener {
    private final RabbitTemplate rabbitTemplate;

    @Value("${spring.rabbitmq.maxRetries}")
    private int maxRetries;

    public AsyncRabbitListener(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitConfig.PROCESSING_QUEUE, concurrency = "3-10")
    public void processingListener(Message message) throws Exception {
        String msgStr = new String(message.getBody(), StandardCharsets.UTF_8);
        long retryCount = getMessageCount(message);

        log.info("\n\n");
        log.info("Received message: {}, count {}", msgStr, retryCount);

        // when retires count exceed then msg in sent to filed queue
        // no further processing happens in that queue
        TimeUnit.SECONDS.sleep(new Random().nextInt(10-3+1) + 3);

        if (retriesExceeded(message)) {
            log.info("Retires over. Sending message {} count {} to failed queue", msgStr, retryCount);
            sendToFailed(message);
            return;
        }
        // processing logic here

        // always throw to simulate retry
        if (true) {
            log.info("exception raised for {}", msgStr);
            throw new Exception();
        }

        log.info("Finished processing {}", message);

    }


    private boolean shouldThrow() {
        return new Random().nextBoolean();
    }

    private long getMessageCount(Message message) {
        List<Map<String, ?>> xDeathHeader = message.getMessageProperties().getXDeathHeader();

        if (xDeathHeader != null && !xDeathHeader.isEmpty()) {
            return (long) xDeathHeader.get(0).get("count");
        }

        return 0;
    }

    private boolean retriesExceeded(Message message) {
        return getMessageCount(message) >= maxRetries;
    }

    private void sendToFailed(Message message) {
        rabbitTemplate.send(RabbitConfig.FAILED_QUEUE, message);
    }
}
