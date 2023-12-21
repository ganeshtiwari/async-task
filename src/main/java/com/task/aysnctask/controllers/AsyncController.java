package com.task.aysnctask.controllers;

import com.task.aysnctask.services.AsyncRabbitProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;

@Slf4j
@RestController
public class AsyncController {

    private final AsyncRabbitProducer asyncRabbitProducer;

    public AsyncController(AsyncRabbitProducer asyncRabbitProducer) {
        this.asyncRabbitProducer = asyncRabbitProducer;
    }

    @PostMapping("/simulate")
    public ResponseEntity<String> simulate() {
        log.info("******** Starting simulation ********");

        int i=1;
        while (i <= 5) {
            asyncRabbitProducer.sendToProcessingQueue("Task " + i);
            i++;
        }
        return new ResponseEntity<>("simulation started", HttpStatus.CREATED);
    }

    private String generateRandomMessage() {
        int leftLimit = 97;
        int rightLimit = 122;
        int targetStringLength = 8;
        Random random = new Random();

        return random.ints(leftLimit, rightLimit + 1)
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

}
