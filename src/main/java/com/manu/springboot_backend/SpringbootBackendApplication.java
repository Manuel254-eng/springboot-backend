package com.manu.springboot_backend;

import com.manu.springboot_backend.model.Task;
import com.manu.springboot_backend.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringbootBackendApplication  implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(SpringbootBackendApplication.class, args);
	}
	@Autowired
	private TaskRepository taskRepository;



	@Override
	public void run(String... args) throws Exception {

	}
}
