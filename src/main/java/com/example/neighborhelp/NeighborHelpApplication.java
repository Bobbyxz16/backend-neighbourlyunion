package com.example.neighborhelp;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class NeighborHelpApplication  {

	public static void main(String[] args) {
		SpringApplication.run(NeighborHelpApplication.class, args);
	}


}
