package com.minn.langchain4j.service;

import com.alibaba.fastjson2.JSONObject;

public interface GaodeMapService {
        
        /**
         * 地理编码：将地址转换为经纬度
         */
        JSONObject geocode(String address);
        
        /**
         * 逆地理编码：将经纬度转换为地址
         */
        JSONObject reverseGeocode(String location);
        
        /**
         * 步行导航路线规划
         */
        JSONObject walkingRoute(String origin, String destination);
        
        /**
         * 驾车导航路线规划
         */
        JSONObject drivingRoute(String origin, String destination);
        
        /**
         * 公交导航路线规划
         */
        JSONObject transitRoute(String origin, String destination, String city);
}
