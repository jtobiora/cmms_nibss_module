package ng.upperlink.nibss.cmms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/*
*   Add a rest template
* */
@Configuration
public class RestClientTemplate {

    @Bean
    public RestTemplate getRestTemplate(){
        return new RestTemplate();
    }
}
