package microservices.service1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/service1")
public class Service1Controller {

    @GetMapping("/hello")
    public String hello() {
        log.info("Service1Controller hello called");
        return "Hello Service 1";
    }
}
