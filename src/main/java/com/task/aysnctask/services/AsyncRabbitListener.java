package com.task.aysnctask.services;

import com.task.aysnctask.configurations.RabbitConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Component
public class AsyncRabbitListener {
    private final RabbitTemplate rabbitTemplate;

    private final ScheduledExecutorService watcherScheduler = Executors.newScheduledThreadPool(1);
    private final Executor jobExecutor = Executors.newScheduledThreadPool(5);
    private final List<AsyncJob> jobs = Collections.synchronizedList(new ArrayList<>());

    private final int MAX_RETRIES = 3;

    @Value("${spring.rabbitmq.maxRetries}")
    private int maxRetries;

    public AsyncRabbitListener(RabbitTemplate rabbitTemplate) {

        this.rabbitTemplate = rabbitTemplate;

        watcherScheduler.scheduleAtFixedRate(this::watchJobs, 0, 45, TimeUnit.SECONDS);
    }

    @RabbitListener(queues = RabbitConfig.PROCESSING_QUEUE, concurrency = "3-10")
    public void processingListener(Message message) {
        String msgStr = new String(message.getBody(), StandardCharsets.UTF_8);
//        long retryCount = getMessageCount(message);

        AsyncJob job = AsyncJob.builder()
                .maxRetries(3)
                .retries(0)
                .expectedCompletionDurationInSec(30)
                .message(msgStr)
                .build();

        jobs.add(job);

        scheduleJob(job);

    }

    /**
     * This function schedules an async job after a fixed delay.
     * If the job wasn't scheduled successfully then, it reschedules the job
     * for {maxRetries} times and then just throws if scheduler was unable to reschedule
     * it again.
     *
     * @param job async job to schedule
     */
    private void scheduleJob(AsyncJob job) {
        log.info("{} is being scheduled", job.getMessage());
        job.setScheduledTime(new Date());
        job.setStatus(AsyncJobStatus.SCHEDULED);

        jobExecutor.execute(job);

    }

    private void watchJobs() {
        log.info("watcher started");
        synchronized (jobs) {
            Iterator<AsyncJob> jobsIterator = jobs.iterator();

            while (jobsIterator.hasNext()) {
                AsyncJob job = jobsIterator.next();

                if (job.isCompleted()) {
                    log.info("{} is complete. Removing from watch list", job.getMessage());
                    jobsIterator.remove();
                } else {
                    if (job.getRetries() >= maxRetries) {
                        // notify of failure to admin
                        // and remove from jobs list
                        jobsIterator.remove();

                    } else {
                        long scheduledDurationMs = new Date().getTime() - job.getScheduledTime().getTime();
                        if (scheduledDurationMs > job.getExpectedCompletionDurationInSec() * 1000L) {
                            // something wrong with the scheduler so
                            // reschedule the job

                            log.info("Retrying {}", job.getMessage());
                            rescheduleJob(job);
                        }
                    }
                }

            }
        }
    }

    private void rescheduleJob(AsyncJob job) {
        job.setRetries(job.getRetries() + 1);
        scheduleJob(job);
    }


//    private boolean shouldThrow() {
//        return new Random().nextBoolean();
//    }

//    private long getMessageCount(Message message) {
//        List<Map<String, ?>> xDeathHeader = message.getMessageProperties().getXDeathHeader();
//
//        if (xDeathHeader != null && !xDeathHeader.isEmpty()) {
//            return (long) xDeathHeader.get(0).get("count");
//        }
//
//        return 0;
//    }

//    private boolean retriesExceeded(Message message) {
//        return getMessageCount(message) >= maxRetries;
//    }
//
//    private void sendToFailed(Message message) {
//        rabbitTemplate.send(RabbitConfig.FAILED_QUEUE, message);
//    }
}
