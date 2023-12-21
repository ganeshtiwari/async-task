package com.task.aysnctask.services;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Setter
@Getter
@Builder
@EqualsAndHashCode
public class AsyncJob implements Runnable {
    @EqualsAndHashCode.Include
    private String message;

    @EqualsAndHashCode.Include
    private Date scheduledTime;
    private Date completionTime;
    private int expectedCompletionDurationInSec;

    private int retries;

    @EqualsAndHashCode.Include
    private int maxRetries;

    private AsyncJobStatus status;

    @Override
    public void run() {
        log.info("Updating {} to running", message);
        setStatus(AsyncJobStatus.RUNNING);

        try {
            TimeUnit.SECONDS.sleep(4);
        } catch (InterruptedException e) {
            // silent
        }

        if (new Random().nextBoolean()) {
            log.info("Exiting {} without completing", message);
            return;
        }

        if (new Random().nextBoolean()) {
            setStatus(AsyncJobStatus.SUCCESS);
        } else {
            setStatus(AsyncJobStatus.ERROR);
        }
        log.info("{} completed", message);
        setCompletionTime(new Date());
    }

    public boolean isCompleted() {
        return AsyncJobStatus.SUCCESS.equals(getStatus())
                || AsyncJobStatus.ERROR.equals(getStatus());
    }
}
