package preserveOther.controller;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;

import edu.fudan.common.util.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import preserveOther.entity.OrderTicketsInfo;
import preserveOther.service.PreserveOtherService;

import static org.springframework.http.ResponseEntity.ok;

/**
 * @author fdse
 */
@RestController
@RequestMapping("/api/v1/preserveotherservice")
public class PreserveOtherController
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PreserveOtherController.class);

    @Autowired
    private PreserveOtherService preserveService;

    @GetMapping(path = "/welcome")
    public String home()
    {
        return "Welcome to [ PreserveOther Service ] !";
    }

    @CrossOrigin(origins = "*")
    @PostMapping(value = "/preserveOther")
    @HystrixCommand(fallbackMethod = "preserveFallback", commandProperties = {
            // changed the timeout value form 5000 to 20000 as the preserve execution consistently takes more than 5 seconds
        @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "20000")
    })
    public HttpEntity preserve(@RequestBody OrderTicketsInfo oti,
        @RequestHeader HttpHeaders headers)
    {
        PreserveOtherController.LOGGER.info("[Preserve Other Service][Preserve] Account  order from {} -----> {} at {}",
            oti.getFrom(), oti.getTo(), oti.getDate());
        return ok(preserveService.preserve(oti, headers));
    }

    private HttpEntity preserveFallback(@RequestBody OrderTicketsInfo oti, @RequestHeader HttpHeaders headers)
    {
        return ok(new Response<>());
    }
}
