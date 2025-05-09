package ua.foxminded.schoolapplication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import ua.foxminded.schoolapplication.testutil.TestcontainersConfiguration;

@SpringBootApplication
public class SchoolApplicationConsole {

	public static void main(String[] args) {
		SpringApplication.from(SchoolApplicationConsole::main).with(TestcontainersConfiguration.class).run(args);
	}

}
