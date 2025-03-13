package adminroute.service;

import adminroute.entity.Route;
import adminroute.entity.RouteInfo;
import edu.fudan.common.util.Response;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * @author fdse
 */
@Service
@Slf4j
public class AdminRouteServiceImpl implements AdminRouteService
{
    @Autowired
    private RestTemplate restTemplate;

    @Value("${ts.route.service.url:ts-route-service}")
    private String tsRouteServiceUrl;

    @Value("${ts.route.service.port:11178}")
    private String tsRouteServicePort;

    @Override
    public Response getAllRoutes(HttpHeaders headers)
    {
        HttpEntity requestEntity = new HttpEntity(headers);
        ResponseEntity<Response> re = restTemplate.exchange(
            "http://"+ tsRouteServiceUrl +":"+ tsRouteServicePort +"/api/v1/routeservice/routes",
            HttpMethod.GET,
            requestEntity,
            Response.class);
        return re.getBody();
    }

    @Override
    public Response createAndModifyRoute(RouteInfo request, HttpHeaders headers)
    {

        HttpEntity requestEntity = new HttpEntity(request, headers);
        ResponseEntity<Response<Route>> re = restTemplate.exchange(
                "http://"+ tsRouteServiceUrl +":"+ tsRouteServicePort +"/api/v1/routeservice/routes",
            HttpMethod.POST,
            requestEntity,
            new ParameterizedTypeReference<Response<Route>>()
            {
            });
        return re.getBody();
    }

    @Override
    public Response deleteRoute(String routeId, HttpHeaders headers)
    {

        HttpEntity requestEntity = new HttpEntity(headers);
        ResponseEntity<Response> re = restTemplate.exchange(
                "http://"+ tsRouteServiceUrl +":"+ tsRouteServicePort +"/api/v1/routeservice/routes/" + routeId,
            HttpMethod.DELETE,
            requestEntity,
            Response.class);
        return re.getBody();
    }
}
