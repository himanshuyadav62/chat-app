package com.example.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Table; 
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import lombok.Data;

@Entity
@Table(indexes = {
    @Index(name = "idx_sender", columnList = "sender"),
    @Index(name = "idx_receiver", columnList = "receiver"),
    @Index(name = "idx_timeStamp", columnList = "timeStamp")
})
@Data
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sender;
    private String receiver;
    private String text;
    private LocalDateTime timeStamp;
}
