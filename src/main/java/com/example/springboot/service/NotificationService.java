package com.example.springboot.service;

/** One implementation per environment; @Profile decides which bean exists. */
public interface NotificationService {

    String channel();
}
