package microservices.service1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/service1")
public class Service1Controller {
    private final RestTemplateBuilder restTemplateBuilder;

    @GetMapping("/hello")
    public String hello() {
        log.info("Service1Controller hello called");
        try {
            ResponseEntity<String> serice2Result = restTemplateBuilder
                    .build()
                    .getForEntity("http://localhost:8081/service2/hello",
                            String.class);

            log.info("service 1 called service 2: {}", serice2Result.getBody());
            
        } catch (ResourceAccessException | HttpClientErrorException e) {
            log.error("Error communicating with service2 service: {}", e.getMessage());
            return "Error";
        }
        return "Hello Service 1";
    }
}
