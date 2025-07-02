package com.example.smppsender.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String content;

    @Column(nullable = false)
    private String destinationNumber;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "smpp_message_id", unique = true)
    private String smppMessageId;

    @Column(length = 20)
    private String senderId;

    @Column(length = 255)
    private String errorMessage; // текст ошибки при отправке

    @Column(length = 20)
    private String status;
}
