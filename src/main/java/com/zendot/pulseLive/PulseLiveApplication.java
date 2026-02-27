package com.zendot.pulseLive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PulseLiveApplication {

	public static void main(String[] args) {
		SpringApplication.run(PulseLiveApplication.class, args);
	}

}
