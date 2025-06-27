package com.example.smppsender.controller;

import com.example.smppsender.service.CsvLoaderService;
import com.example.smppsender.service.MessageSenderService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageSenderService messageSenderService;
    private final CsvLoaderService csvLoaderService;

    public MessageController(MessageSenderService messageSenderService, CsvLoaderService csvLoaderService) {
        this.messageSenderService = messageSenderService;
        this.csvLoaderService = csvLoaderService;
    }

    @PostMapping("/csv-upload")
    public String sendMessagesFromCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam("message") String content
    ) throws IOException {
        List<String> numbers = csvLoaderService.extractNumbers(file);
        messageSenderService.sendMessageToMany(content, numbers);
        return "Запущена отправка " + numbers.size() + " сообщений.";
    }

    @PostMapping("/send")
    public String sendMessages(
            @RequestParam String destinationNumber,
            @RequestParam String content,
            @RequestParam int count
    ) {
        messageSenderService.sendBulkMessages(destinationNumber, content, count);
        return "Отправка " + count + " сообщений запущена.";
    }
}
