package com.example.neighborhelp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponseRequest {
    private String message;
    private Object data;

    public MessageResponseRequest(String message) {
        this.message = message;
    }
}