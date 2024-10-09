package com.example.backend.service;

import com.example.backend.entity.Message;
import com.example.backend.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {
    private final MessageRepository messageRepository;
    private final AsyncMessageService asyncMessageService;
    private final BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<Message> retryQueue = new LinkedBlockingQueue<>();

    @Async("taskExecutor")
    public void addToQueue(Message message) {
        messageQueue.add(message);
        // Call the async service to process the queue asynchronously
        asyncMessageService.processMessageQueue(messageQueue, retryQueue);
    }

    @Scheduled(fixedDelay = 1000000)  // Run every 1000 seconds
    public void retryFailedMessages() {
        log.info("Retrying failed messages...");
        while (!retryQueue.isEmpty()) {
            Message message = retryQueue.poll();
            if (message != null) {
                try {
                    messageRepository.save(message);
                    log.info("Retried message saved successfully: {}", message);
                } catch (Exception e) {
                    log.error("Retry failed, will try again later: {}", message, e);
                    retryQueue.add(message); 
                }
            }
        }
    }
}

@Service
@RequiredArgsConstructor
class AsyncMessageService {
    private final MessageRepository messageRepository;

    private final Logger logger = LoggerFactory.getLogger(AsyncMessageService.class);

    @Async("taskExecutor")
    public void processMessageQueue(BlockingQueue<Message> messageQueue, BlockingQueue<Message> retryQueue) {
        while (!messageQueue.isEmpty()) {
            Message message = messageQueue.poll();
            if (message != null) {
                try {
                    messageRepository.save(message);
                } catch (Exception e) {
                    logger.error("Failed to save message, adding to retry queue: {}", message, e);
                    retryQueue.add(message);  // Add to retry queue in case of failure
                }
            }
        }
    }
}