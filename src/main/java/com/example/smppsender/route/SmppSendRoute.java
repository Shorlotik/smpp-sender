package com.example.smppsender.route;

import com.example.smppsender.model.Message;
import com.example.smppsender.repository.MessageRepository;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.smpp.SmppConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SmppSendRoute extends RouteBuilder {

    private static final Logger logger = LoggerFactory.getLogger(SmppSendRoute.class);
    private final MessageRepository messageRepository;

    @Value("${smpp.sender}")
    private String defaultSender; // Пример: BK.bel

    public SmppSendRoute(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public void configure() throws Exception {

        // Обработка исключений — сохраняем ошибку в БД, меняем статус
        onException(Exception.class)
                .process(exchange -> {
                    Throwable exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);
                    Message message = exchange.getIn().getBody(Message.class);
                    if (message != null) {
                        message.setStatus("FAILED");
                        message.setErrorMessage(exception.getMessage());
                        messageRepository.save(message);
                    }
                    logger.error("Ошибка при отправке SMPP сообщения: {}", exception.getMessage(), exception);
                })
                .handled(true);

        from("direct:sendSmppMessage")
                .process(exchange -> {
                    Message message = exchange.getIn().getBody(Message.class);
                    message.setCreatedAt(LocalDateTime.now());

                    // Если senderId не задан, ставим дефолтный
                    if (message.getSenderId() == null || message.getSenderId().isBlank()) {
                        message.setSenderId(defaultSender);
                    }

                    // Проверка длины senderId для SMPP (до 20 символов)
                    if (message.getSenderId().length() > 20) {
                        throw new IllegalArgumentException("senderId превышает 20 символов: " + message.getSenderId());
                    }

                    // Сохраняем сообщение (статус и ошибка пока не меняем)
                    messageRepository.save(message);

                    // Устанавливаем заголовки SMPP и тело
                    exchange.getIn().setHeader(SmppConstants.SOURCE_ADDR, message.getSenderId());
                    exchange.getIn().setHeader(SmppConstants.DEST_ADDR, message.getDestinationNumber());
                    exchange.getIn().setHeader(SmppConstants.DATA_CODING, (byte) 0x08); // UCS-2
                    exchange.getIn().setBody(message.getContent());

                    logger.info("Подготовлено сообщение к отправке: id={}, to={}, sender={}, content='{}'",
                            message.getId(), message.getDestinationNumber(), message.getSenderId(), message.getContent());
                })
                .to("smpp://{{camel.component.smpp.systemId}}@{{camel.component.smpp.host}}:{{camel.component.smpp.port}}"
                        + "?password={{camel.component.smpp.password}}"
                        + "&enquireLinkTimer={{camel.component.smpp.enquireLinkTimer}}"
                        + "&transactionTimer={{camel.component.smpp.transactionTimer}}"
                        + "&systemType={{camel.component.smpp.systemType}}")
                .process(exchange -> {
                    String smppMessageId = exchange.getIn().getHeader(SmppConstants.ID, String.class);
                    Message message = exchange.getIn().getBody(Message.class);

                    if (smppMessageId != null) {
                        message.setSmppMessageId(smppMessageId);
                    }
                    // Помечаем как отправленное и очищаем ошибку
                    message.setStatus("SENT");
                    message.setErrorMessage(null);

                    messageRepository.save(message);

                    logger.info("Сообщение отправлено успешно: id={}, smppMessageId={}", message.getId(), smppMessageId);
                });
    }
}
