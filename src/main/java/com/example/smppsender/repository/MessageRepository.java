package com.example.smppsender.repository;

import com.example.smppsender.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    Message findBySmppMessageId(String smppMessageId);
}
