package com.example.backend.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.backend.entity.Message;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m WHERE m.sender = ?1 AND m.receiver = ?2 OR m.sender = ?2 AND m.receiver = ?1 ORDER BY m.timeStamp DESC")
    List<Message> findBySenderAndReceiverOrderByTimeStampDesc(String currentUserEmail, String user);
}
