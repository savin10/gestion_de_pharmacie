package com.pharmacie.benin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PharmacieBeninApplication {

	public static void main(String[] args) {
		SpringApplication.run(PharmacieBeninApplication.class, args);
	}

}
