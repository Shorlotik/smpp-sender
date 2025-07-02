package com.example.smppsender.service;

import com.example.smppsender.model.Message;
import org.apache.camel.ProducerTemplate;
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

    // Проверка формата номера для Беларуси (формат 375XXYYYYYYY)
    private boolean isValidBelarusNumber(String number) {
        // Операторы Беларуси: 25,29,33,44,17 и 7 цифр после кода
        return number != null && number.matches("^375(25|29|33|44|17)\\d{7}$");
    }

    public void sendBulkMessages(String destinationNumber, String content, int totalMessages) {
        if (!isValidBelarusNumber(destinationNumber)) {
            logger.warn("Отмена отправки: номер не валиден для Беларуси: {}", destinationNumber);
            return;
        }

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
                });
            });
        }
    }

    public void sendMessageToMany(String text, List<String> numbers) {
        for (String number : numbers) {
            if (!isValidBelarusNumber(number)) {
                logger.warn("Пропущен номер невалидного формата: {}", number);
                continue;
            }

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
                });
            });
        }
    }
}
