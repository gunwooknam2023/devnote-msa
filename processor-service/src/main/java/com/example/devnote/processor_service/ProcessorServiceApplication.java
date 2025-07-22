package com.example.devnote.processor_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableDiscoveryClient
public class ProcessorServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProcessorServiceApplication.class, args);
	}

}
