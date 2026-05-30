package com.skycrew;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SkycrewApplication {

	public static void main(String[] args) {
		SpringApplication.run(SkycrewApplication.class, args);
	}

}
