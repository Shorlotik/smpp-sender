package com.example.smppsender.service;

import com.example.smppsender.model.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.smpp.SmppConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class MessageSenderService {

    private static final Logger logger = LoggerFactory.getLogger(MessageSenderService.class);

    private final ProducerTemplate producerTemplate;

    private final ExecutorService executor = Executors.newFixedThreadPool(20);

    @Value("${smpp.sender}")
    private String defaultSender;

    public MessageSenderService(ProducerTemplate producerTemplate) {
        this.producerTemplate = producerTemplate;
    }

    public void sendBulkMessages(String destinationNumber, String content, int totalMessages) {
        for (int i = 0; i < totalMessages; i++) {
            final int index = i;
            executor.submit(() -> {
                Message message = Message.builder()
                        .content(content + " #" + index)
                        .destinationNumber(destinationNumber)
                        .senderId(defaultSender)
                        .createdAt(LocalDateTime.now())
                        .build();

                logger.info("Запуск отправки сообщения №{} на номер {}: '{}'", index, destinationNumber, message.getContent());

                producerTemplate.send("direct:sendSmppMessage", exchange -> {
                    exchange.getIn().setBody(message);
                    exchange.getIn().setHeader(SmppConstants.SOURCE_ADDR, defaultSender);
                });
            });
        }
    }

    public void sendMessageToMany(String text, List<String> numbers) {
        for (String number : numbers) {
            executor.submit(() -> {
                Message message = Message.builder()
                        .content(text)
                        .destinationNumber(number)
                        .senderId(defaultSender)
                        .createdAt(LocalDateTime.now())
                        .build();

                logger.info("Запуск отправки сообщения на номер {}: '{}'", number, text);

                producerTemplate.send("direct:sendSmppMessage", exchange -> {
                    exchange.getIn().setBody(message);
                    exchange.getIn().setHeader(SmppConstants.SOURCE_ADDR, defaultSender);
                });
            });
        }
    }
}
