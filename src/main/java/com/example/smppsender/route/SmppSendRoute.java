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
    private String defaultSender;

    public SmppSendRoute(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public void configure() throws Exception {

        // Логируем исключения, если они будут
        onException(Exception.class)
                .log(LoggingLevel.ERROR, logger, "Ошибка при отправке SMPP сообщения: ${exception.message}")
                .handled(true);

        from("direct:sendSmppMessage")
                .process(exchange -> {
                    Message message = exchange.getIn().getBody(Message.class);
                    message.setCreatedAt(LocalDateTime.now());
                    messageRepository.save(message);
                    logger.info("Подготовлено сообщение к отправке: id={}, to={}, sender={}, content='{}'",
                            message.getId(), message.getDestinationNumber(), message.getSenderId(), message.getContent());

                    // Гарантируем, что senderId всегда установлен
                    if (message.getSenderId() == null || message.getSenderId().isBlank()) {
                        message.setSenderId(defaultSender); // defaultSender = "BK.bel"
                    }

                    // Ограничим длину отправителя для соответствия SMPP
                    if (message.getSenderId().length() > 20) {
                        throw new IllegalArgumentException("senderId превышает 20 символов: " + message.getSenderId());
                    }
                })
                .setHeader(SmppConstants.DATA_CODING, constant((byte) 0x08)) // UCS-2
                .setHeader(SmppConstants.SOURCE_ADDR, simple("${body.senderId}"))
                .setHeader(SmppConstants.DEST_ADDR, simple("${body.destinationNumber}"))
                .setBody(simple("${body.content}"))
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
                        messageRepository.save(message);
                        logger.info("Сообщение отправлено успешно: id={}, smppMessageId={}", message.getId(), smppMessageId);
                    } else {
                        logger.warn("SMPP сообщение отправлено, но smppMessageId не получен: id={}", message.getId());
                    }
                });
    }
}