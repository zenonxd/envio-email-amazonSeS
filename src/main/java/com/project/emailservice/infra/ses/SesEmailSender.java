package com.project.emailservice.infra.ses;

import com.project.emailservice.adapters.EmailSenderGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Service
public class SesEmailSender implements EmailSenderGateway {

    private SesClient sesClient;


    @Autowired
    public SesEmailSender(SesClient sesClient) {
        this.sesClient = sesClient;
    }

    public void sendEmail(String to, String subject, String body) {
        SendEmailRequest request = SendEmailRequest.builder()
                .destination(Destination.builder()
                        .toAddresses(to) //endereço destino
                        .build())
                .message(Message.builder()
                        .subject(Content.builder()
                                .data(subject) //assunto
                                .build())
                        .body(Body.builder()
                                .text(Content.builder()
                                        .data(body)
                                        .build())
                                .build())
                        .build())
                .source("olavomoreiracontato@gmail.com")
                .build();
        try {
            sesClient.sendEmail(request);
            System.out.println("Email sent successfully");
        } catch (SesException e) {
            System.out.println("Email sent failed: " + e.awsErrorDetails().errorMessage() );
        }
    }
}
