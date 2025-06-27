package com.example.smppsender.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CsvLoaderService {

    /**
     * Извлекает номера телефонов из CSV-файла.
     * Ожидает по одному номеру на строку. Валидирует по-простому regex.
     */
    public List<String> extractNumbers(MultipartFile file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            return reader.lines()
                    .map(String::trim)
                    .filter(line -> line.matches("^\\+?\\d{10,15}$")) // простой валидатор номера
                    .distinct() // убрать дубликаты
                    .collect(Collectors.toList());
        }
    }
}
