package com.banda.barbershop;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@EnableScheduling
public class BarbershopBookingApplication {

	public static void main(String[] args) {
		Map<String, Object> dotenvProperties = new HashMap<>();
		try {
			Dotenv dotenv = Dotenv.configure()
					.ignoreIfMissing()
					.load();
			dotenv.entries().forEach(entry -> {
				dotenvProperties.put(entry.getKey(), entry.getValue());
			});
		} catch (Exception e) {
			System.err.println("⚠️ Could not load .env file: " + e.getMessage());
			e.printStackTrace();
		}

		SpringApplication app = new SpringApplication(BarbershopBookingApplication.class);
		app.addInitializers(context -> {
			ConfigurableEnvironment env = context.getEnvironment();
			env.getPropertySources().addFirst(new MapPropertySource("dotenvProperties", dotenvProperties));
		});
		app.run(args);
	}

}
