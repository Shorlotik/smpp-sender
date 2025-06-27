package com.example.smppsender.route;

import com.example.smppsender.model.DeliveryStatus;
import com.example.smppsender.model.Message;
import com.example.smppsender.repository.DeliveryStatusRepository;
import com.example.smppsender.repository.MessageRepository;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.smpp.SmppConstants;
import org.springframework.stereotype.Component;
import com.example.smppsender.model.DeliveryStatusCode;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class SmppDlqRoute extends RouteBuilder {

    private final DeliveryStatusRepository deliveryStatusRepository;
    private final MessageRepository messageRepository;

    public SmppDlqRoute(DeliveryStatusRepository deliveryStatusRepository,
                        MessageRepository messageRepository) {
        this.deliveryStatusRepository = deliveryStatusRepository;
        this.messageRepository = messageRepository;
    }

    @Override
    public void configure() throws Exception {
        from("smpp://{{camel.component.smpp.systemId}}@{{camel.component.smpp.host}}:{{camel.component.smpp.port}}"
                + "?password={{camel.component.smpp.password}}"
                + "&enquireLinkTimer={{camel.component.smpp.enquireLinkTimer}}"
                + "&transactionTimer={{camel.component.smpp.transactionTimer}}"
                + "&systemType={{camel.component.smpp.systemType}}"
                + "&receiveListenerEnabled=true")
                .routeId("smpp-dlr-route")
                .process(exchange -> {
                    Map<String, Object> headers = exchange.getIn().getHeaders();

                    String smppMessageId = (String) headers.get(SmppConstants.ID);
                    String messageStateRaw = (String) headers.get(SmppConstants.MESSAGE_STATE);
                    String dlrMessage = exchange.getIn().getBody(String.class);

                    Message message = null;
                    if (smppMessageId != null) {
                        message = messageRepository.findBySmppMessageId(smppMessageId);
                    }

                    DeliveryStatusCode statusCode = DeliveryStatusCode.fromString(messageStateRaw);

                    if (message == null) {
                        // Сообщение не найдено, логируем и продолжаем
                        // Можно добавить логирование или мониторинг
                        return;
                    }

                    DeliveryStatus status = DeliveryStatus.builder()
                            .message(message)
                            .statusCode(statusCode.name())
                            .statusDescription(dlrMessage)
                            .receivedAt(LocalDateTime.now())
                            .build();
                    deliveryStatusRepository.save(status);

                    // Дополнительная логика, если нужна, например:
                    switch (statusCode) {
                        case ACCEPTD:
                            // Промежуточный статус — можно логировать
                            break;
                        case DELIVRD:
                            // Сообщение доставлено — можно обновить статус сообщения
                            break;
                        case UNDELIV:
                        case FAILED:
                        case EXPIRED:
                        case REJECTD:
                            // Ошибка доставки — логируем, уведомляем, retry и т.д.
                            break;
                        case DELETED:
                            // Особый статус — можно логировать и поднять тикет
                            break;
                        case UNKNOWN:
                        default:
                            // Неизвестный статус — сохраняем, но ничего не ломаем
                            break;
                    }
                });
    }
}
