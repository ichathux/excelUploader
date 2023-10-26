package com.controlunion.excelUploader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class ExcelUploaderApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExcelUploaderApplication.class, args);
	}

}
