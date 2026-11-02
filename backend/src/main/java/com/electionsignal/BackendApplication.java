package com.electionsignal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		var context = SpringApplication.run(BackendApplication.class, args);
		if (java.util.Arrays.stream(args).anyMatch(a -> a.startsWith("--election.forecast-import=") || a.startsWith("--election.survey-import=") || a.startsWith("--election.medsl-senate-import=") || a.startsWith("--election.ca-results-preflight=") || a.startsWith("--election.nc-results-preflight="))) context.close();
	}

}
