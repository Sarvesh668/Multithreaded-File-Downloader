package com.example.filedowloader.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.example.filedowloader.demo.service.DownloadManagerService;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	/*public CommandLineRunner runTest(DownloadManagerService downloadService){
		return args->{
			System.out.println("====== SERVER STARTED: RUNNING TEST ======");
            // We pass in a dummy PDF from the W3C consortium to test our math
            String testUrl = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf";
            downloadService.startDownload(testUrl);
            System.out.println("====== TEST COMPLETE ======");
		};
	}*/
}
