package ng.upperlink.nibss.cmms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableAsync;

/*
* Application main class
*
* */
@SpringBootApplication
@EnableAsync
public class CmmsNibssApplication extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(
			SpringApplicationBuilder builder) {
		return builder.sources(CmmsNibssApplication.class);
	}

	public static void main(String[] args) {
		SpringApplication.run(CmmsNibssApplication.class, args);
	}

//	@Bean
//	public ContentFilter replaceHtmlFilter() {
//		return new ContentFilter();
//	}

}
