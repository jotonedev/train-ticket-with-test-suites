package preserve.controller;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;

import edu.fudan.common.util.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import preserve.entity.*;
import preserve.service.PreserveService;

import static org.springframework.http.ResponseEntity.ok;

/**
 * @author fdse
 */
@RestController
@RequestMapping("/api/v1/preserveservice")
public class PreserveController
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PreserveController.class);

    @Autowired
    private PreserveService preserveService;

    @GetMapping(path = "/welcome")
    public String home()
    {
        return "Welcome to [ Preserve Service ] !";
    }

    @CrossOrigin(origins = "*")
    @PostMapping(value = "/preserve")
    @HystrixCommand(fallbackMethod = "preserveFallback", commandProperties = {
            // changed the timeout value form 5000 to 20000 as the preserve execution consistently takes more than 5 seconds
        @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "20000")
    })
    public HttpEntity preserve(@RequestBody OrderTicketsInfo oti,
        @RequestHeader HttpHeaders headers)
    {
        PreserveController.LOGGER.info("[Preserve Service][Preserve] Account  order from {} -----> {} at {}",
            oti.getFrom(), oti.getTo(), oti.getDate());
        return ok(preserveService.preserve(oti, headers));
    }

    private HttpEntity preserveFallback(@RequestBody OrderTicketsInfo oti, @RequestHeader HttpHeaders headers)
    {
        return ok(new Response<>());
    }
}
