package com.example.devnote.news_youtube_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
public class NewsYoutubeServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(NewsYoutubeServiceApplication.class, args);
	}

}
