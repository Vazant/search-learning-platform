package com.example.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // Включаем планировщик для автоматического анализа запросов
public class SearchDemoApplication {

  public static void main(String[] args) {
    SpringApplication.run(SearchDemoApplication.class, args);
  }
}
