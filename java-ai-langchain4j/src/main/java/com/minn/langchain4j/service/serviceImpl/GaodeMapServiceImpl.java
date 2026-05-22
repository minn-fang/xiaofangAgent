package com.minn.langchain4j.service.serviceImpl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.minn.langchain4j.service.GaodeMapService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GaodeMapServiceImpl implements GaodeMapService {
    
    @Value("${gaode.api.key}")
    private String apiKey;
    
    @Value("${gaode.api.url}")
    private String baseUrl;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Override
    public JSONObject geocode(String address) {
        String url = baseUrl + "/geocode/geo?address=" + address + "&key=" + apiKey;
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        return JSON.parseObject(response.getBody());
    }
    
    @Override
    public JSONObject reverseGeocode(String location) {
        String url = baseUrl + "/geocode/regeo?location=" + location + "&key=" + apiKey;
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        return JSON.parseObject(response.getBody());
    }
    
    @Override
    public JSONObject walkingRoute(String origin, String destination) {
        String url = baseUrl + "/direction/walking?origin=" + origin + "&destination=" + destination + "&key=" + apiKey;
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        return JSON.parseObject(response.getBody());
    }
    
    @Override
    public JSONObject drivingRoute(String origin, String destination) {
        String url = baseUrl + "/direction/driving?origin=" + origin + "&destination=" + destination + "&key=" + apiKey;
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        return JSON.parseObject(response.getBody());
    }
    
    @Override
    public JSONObject transitRoute(String origin, String destination, String city) {
        String url = baseUrl + "/direction/transit/integrated?origin=" + origin + 
                     "&destination=" + destination + "&city=" + city + "&key=" + apiKey;
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        return JSON.parseObject(response.getBody());
    }
}
