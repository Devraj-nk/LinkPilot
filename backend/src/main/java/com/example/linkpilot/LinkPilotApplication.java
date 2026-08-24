package com.example.linkpilot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class LinkPilotApplication {

	public static void main(String[] args) {
		SpringApplication.run(LinkPilotApplication.class, args);
	}

}