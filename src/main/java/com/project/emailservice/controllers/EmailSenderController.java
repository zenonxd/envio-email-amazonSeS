package com.project.emailservice.controllers;

import com.project.emailservice.application.EmailSenderService;
import com.project.emailservice.dto.EmailRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.ses.model.SesException;

@RestController
@RequestMapping("/api/email")
public class EmailSenderController {

    @Autowired
    private EmailSenderService emailSenderService;

    @PostMapping
    public ResponseEntity<String> sendEmail(@RequestBody EmailRequest emailRequest) {

        try {
            this.emailSenderService.sendEmail(
                    emailRequest.to(),
                    emailRequest.body(),
                    emailRequest.subject()
            );

            return ResponseEntity.ok("Email sent successfully");
        } catch (SesException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

    }
}
