package com.movieuniverse.hub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MovieUniverseHubApplication {

	public static void main(String[] args) {
		var context = SpringApplication.run(
				MovieUniverseHubApplication.class,
				args
		);

		if (context.getEnvironment().getProperty(
				"app.seed.import-only",
				Boolean.class,
				false
		)) {
			context.close();
		}
	}

}
