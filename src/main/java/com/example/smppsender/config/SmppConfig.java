package com.example.smppsender.config;

import lombok.Getter;
import lombok.Setter;
import org.apache.camel.component.smpp.SmppConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SmppConfig {

    @Value("${camel.component.smpp.host}")
    private String host;

    @Value("${camel.component.smpp.port}")
    private long port;

    @Value("${camel.component.smpp.systemId}")
    private String systemId;

    @Setter
    @Getter
    @Value("${smpp.sender}")
    private String senderId;

    @Value("${camel.component.smpp.password}")
    private String password;

    @Value("${camel.component.smpp.systemType}")
    private String systemType;

    @Value("${camel.component.smpp.enquireLinkTimer}")
    private long enquireLinkTimer;

    @Value("${camel.component.smpp.transactionTimer}")
    private long transactionTimer;

    @Setter
    @Getter
    @Value("${camel.component.smpp.encoding:UCS2}") // По умолчанию UCS2
    private String encoding;


    @Bean
    public SmppConfiguration smppConfiguration() {
        SmppConfiguration config = new SmppConfiguration();
        config.setHost(host);
        config.setPort((int) port);
        config.setSystemId(systemId);
        config.setPassword(password);
        config.setSystemType(systemType);
        config.setEnquireLinkTimer((int) enquireLinkTimer);
        config.setTransactionTimer((int) transactionTimer);
        config.setEncoding(encoding);
        return config;
    }
}
