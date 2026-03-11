package com.rnhappysmile.java_labs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import de.codecentric.boot.admin.server.config.EnableAdminServer;

@EnableAdminServer
@SpringBootApplication
public class JavaLabsApplication {

	public static void main(String[] args) {
		SpringApplication.run(JavaLabsApplication.class, args);
	}

}
