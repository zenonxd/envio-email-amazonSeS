package com.project.emailservice.dto;

public record EmailRequest(String to, String subject, String body) {
}
