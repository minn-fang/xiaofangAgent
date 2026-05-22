package com.minn.langchain4j.tools;

import com.alibaba.fastjson2.JSONObject;
import com.minn.langchain4j.entity.Appointment;
import com.minn.langchain4j.entity.DoctorSchedule;
import com.minn.langchain4j.service.AppointmentService;
import com.minn.langchain4j.service.DoctorScheduleService;
import com.minn.langchain4j.service.GaodeMapService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
@Slf4j
@Component
public class AppointmentTools {
        @Autowired
        private AppointmentService appointmentService;
        
        @Autowired
        private DoctorScheduleService doctorScheduleService;
        
        @Autowired
        private GaodeMapService gaodeMapService;
        
        @Tool (name="预约挂号", value = "根据参数，先执行工具方法queryDepartment查询是否可预约，并直接给用户回答是否可预约，并让用户确认所有预约信息，用户确认后再进行预约。")
        public String bookAppointment(Appointment appointment){
                // 查找数据库中是否包含对应的预约记录
                Appointment appointmentDB = appointmentService.getOne(appointment);
                if(appointmentDB == null){
                        // 检查是否有可用号源
                        boolean hasSlots = doctorScheduleService.hasAvailableSlots(
                                appointment.getDepartment(),
                                appointment.getDate(),
                                appointment.getTime(),
                                appointment.getDoctorName()
                        );
                        
                        if (!hasSlots) {
                                return "该科室/医生在指定日期和时间没有可用号源，请选择其他时间或医生";
                        }
                        
                        // 预约号源
                        if (doctorScheduleService.bookSlot(
                                appointment.getDepartment(),
                                appointment.getDate(),
                                appointment.getTime(),
                                appointment.getDoctorName()
                        )) {
                                appointment.setId(null);//防止大模型幻觉设置了id
                                if(appointmentService.save(appointment)){
                                        return "预约成功，预约详情：科室-" + appointment.getDepartment() +
                                                       "，日期-" + appointment.getDate() +
                                                       "，时间-" + appointment.getTime() +
                                                       "，医生-" + appointment.getDoctorName();
                                } else {
                                        // 如果保存预约记录失败，回退号源
                                        doctorScheduleService.cancelSlot(
                                                appointment.getDepartment(),
                                                appointment.getDate(),
                                                appointment.getTime(),
                                                appointment.getDoctorName()
                                        );
                                        return "预约失败";
                                }
                        } else {
                                return "预约失败，号源已被预约";
                        }
                }
                return "您在相同的科室和时间已有预约";
        }
        
        @Tool(name="取消预约挂号", value = "根据参数，查询预约是否存在，如果存在则删除预约记录并返回取消预约成功，否则返回取消预约失败")
        public String cancelAppointment(Appointment appointment){
                Appointment appointmentDB = appointmentService.getOne(appointment);
                if(appointmentDB != null){
                        // 释放号源
                        doctorScheduleService.cancelSlot(
                                appointmentDB.getDepartment(),
                                appointmentDB.getDate(),
                                appointmentDB.getTime(),
                                appointmentDB.getDoctorName()
                        );
                        
                        //删除预约记录
                        if(appointmentService.removeById(appointmentDB.getId())){
                                return "取消预约成功";
                        }else{
                                return "取消预约失败";
                        }
                }
                //取消失败
                return "您没有预约记录，请核对预约科室和时间";
        }
        
        @Tool(name = "查询是否有号源", value="根据科室名称，日期，时间和医生查询是否有号源，并返回给用户")
        public boolean queryDepartment(
                @P(value = "科室名称") String name,
                @P(value = "日期") String date,
                @P (value = "时间，可选值：上午、下午") String time,
                @P(value = "医生名称", required = false) String doctorName
        ) {
                System.out.println("查询是否有号源");
                System.out.println("科室名称：" + name);
                System.out.println("日期：" + date);
                System.out.println("时间：" + time);
                System.out.println("医生名称：" + doctorName);
                
                return doctorScheduleService.hasAvailableSlots(name, date, time, doctorName);
        }
        
        @Tool(name = "获取医生排班信息", value = "根据科室名称、日期和时间获取医生排班信息（每天相同时间段可以是相同医生值班），包括医生姓名、总号源数和剩余号源数")
        public String getDoctorScheduleInfo(
                @P(value = "科室名称") String department,
                @P(value = "日期") String date,
                @P(value = "时间，可选值：上午、下午") String time
        ) {
                DoctorSchedule schedule = doctorScheduleService.getSchedule(department, date, time, null);
                if (schedule != null) {
                        int availableSlots = schedule.getTotalSlots() - schedule.getBookedSlots();
                        return "科室：" + schedule.getDepartment() +
                                       "，医生：" + schedule.getDoctorName() +
                                       "，时间：" + schedule.getTime() +
                                       "，总号源数：" + schedule.getTotalSlots() +
                                       "，剩余号源数：" + availableSlots;
                }
                return "未找到该科室在指定日期和时间的排班信息";
        }
        
        
        // ================== 导航相关工具 ==================
        @Tool(name = "查询医院位置", value = "根据医院名称查询医院的详细地址和经纬度坐标。返回格式必须是：{地址:xxx, 经纬度:xxx}")
        public String queryHospitalLocation(
                @P(value = "地点名称，例如：华西医院、四川大学华西医院") String locationName) {
                
                log.info("🔍 正在查询位置: {}", locationName);
                try {
                        JSONObject result = gaodeMapService.geocode(locationName);
                        
                        if ("1".equals(result.getString("status")) && result.getJSONArray("geocodes").size() > 0) {
                                JSONObject geocodes = result.getJSONArray("geocodes").getJSONObject(0);
                                String location = geocodes.getString("location"); // 经纬度
                                String address = geocodes.getString("formatted_address"); // 详细地址
                                
                                // 【关键修改】只返回简洁的结构化数据，不要加“查询成功”等前缀
                                return String.format("地址: %s, 经纬度: %s", address, location);
                        } else {
                                // 如果没查到，直接返回空或者简单的错误码，不要返回长文本
                                return "ERROR: NOT_FOUND";
                        }
                } catch (Exception e) {
                        log.error("查询异常", e);
                        return "ERROR: SYSTEM_ERROR";
                }
        }
        
        @Tool(name = "获取导航路线", value = "根据起点和终点的经纬度坐标获取导航路线。返回格式：距离:xx米, 时间:xx分钟, 路线详情...")
        public String getNavigationRoute(
                @P(value = "起点经纬度，格式：经度,纬度") String origin,
                @P(value = "终点经纬度，格式：经度,纬度") String destination,
                @P(value = "导航方式：walking, driving, transit") String mode) {
                
                log.info("🚗 导航: {} -> {}", origin, destination);
                
                try {
                        String routeInfo = "";
                        switch (mode.toLowerCase()) {
                                case "walking": routeInfo = parseRouteResult(gaodeMapService.walkingRoute(origin, destination), "步行"); break;
                                case "driving": routeInfo = parseRouteResult(gaodeMapService.drivingRoute(origin, destination), "驾车"); break;
                                case "transit": routeInfo = parseTransitResult(gaodeMapService.transitRoute(origin, destination, "北京")); break;
                                default: return "ERROR: INVALID_MODE";
                        }
                        
                        // 【关键修改】直接返回解析后的数据，不要加“为您规划路线”等废话
                        return routeInfo;
                        
                } catch (Exception e) {
                        log.error("导航失败", e);
                        return "ERROR: ROUTE_FAILED";
                }
        }
        
        // 提取解析逻辑，确保格式统一
        private String parseRouteResult(JSONObject result, String type) {
                if ("1".equals(result.getString("status"))) {
                        JSONObject route = result.getJSONObject("route").getJSONArray("paths").getJSONObject(0);
                        int distance = Integer.parseInt(route.getString("distance"));
                        int duration = Integer.parseInt(route.getString("duration")) / 60; // 转换为分钟
                        
                        // 【关键修改】返回极简格式
                        return String.format("%s: 距离%d米, 时间%d分钟", type, distance, duration);
                }
                return type + ": 获取失败";
        }
        
        private String parseTransitResult(JSONObject result) {
                if ("1".equals(result.getString("status"))) {
                        JSONObject transit = result.getJSONObject("route").getJSONArray("transits").getJSONObject(0);
                        int duration = Integer.parseInt(transit.getString("duration")) / 60;
                        int transfers = transit.getIntValue("transfers");
                        
                        // 【关键修改】只返回核心数据
                        return String.format("公交: 时间%d分钟, 换乘%d次", duration, transfers);
                }
                return "公交: 获取失败";
        }
        
        @Tool(name = "获取全方式导航方案", value = "一次性获取步行、驾车、公交三种导航方式的完整对比方案。输入中文地址或经纬度，返回标准JSON格式的距离(米)、时间(分钟)、费用(元)等详细信息。")
        public String getAllNavigationOptions(
                @P(value = "起点，支持中文地址或经纬度") String origin,
                @P(value = "终点，支持中文地址或经纬度") String destination) {
                
                log.info("🔧 工具被调用: getAllNavigationOptions, origin={}, destination={}", origin, destination);
                
                // 构建标准的 JSON 响应对象，而不是拼接字符串
                JSONObject result = new JSONObject();
                
                try {
                        // 1. 获取三种方式的原始数据
                        JSONObject drivingData = parseDrivingRoute(getDrivingRoute(origin, destination));
                        JSONObject walkingData = parseWalkingRoute(getWalkingRoute(origin, destination));
                        JSONObject transitData = parseTransitRoute(getTransitRoute(origin, destination));
                        
                        // 2. 组装 JSON
                        result.put("驾车", drivingData);
                        result.put("步行", walkingData);
                        result.put("公交", transitData);
                        
                        // 计算骑行（基于步行数据）
                        if (walkingData.getBoolean("success")) {
                                JSONObject cycling = new JSONObject();
                                cycling.put("success", true);
                                cycling.put("distance", walkingData.getIntValue("distance"));
                                cycling.put("duration", walkingData.getIntValue("duration") / 3); // 骑行时间约为步行1/3
                                cycling.put("cost", "免费");
                                cycling.put("note", "适合中短途，锻炼身体");
                                result.put("骑行", cycling);
                        }
                        
                        log.info("✅ 导航数据获取成功: {}", result.toJSONString());
                        return result.toJSONString(); // 直接返回 JSON 字符串
                        
                } catch (Exception e) {
                        log.error("💥 获取全方式导航失败: {}", e.getMessage(), e);
                        // 即使出错，也返回标准格式
                        result.put("error", "导航服务暂时不可用: " + e.getMessage());
                        return result.toJSONString();
                }
        }
        
        // --- 私有解析方法 ---
        
        private JSONObject parseDrivingRoute(String rawRoute) {
                JSONObject obj = new JSONObject();
                obj.put("success", false);
                
                if (rawRoute != null && rawRoute.contains("距离") && rawRoute.contains("时间")) {
                        try {
                                // 提取数字
                                int distance = extractDistance(rawRoute);
                                int duration = extractDuration(rawRoute);
                                
                                obj.put("success", true);
                                obj.put("distance", distance);
                                obj.put("duration", duration);
                                obj.put("cost", calculateTaxiFee(distance)); // 可以计算打车费
                                return obj;
                        } catch (Exception e) {
                                obj.put("msg", "解析驾车数据失败");
                        }
                }
                obj.put("msg", "无驾车路线");
                return obj;
        }
        
        private JSONObject parseWalkingRoute(String rawRoute) {
                JSONObject obj = new JSONObject();
                obj.put("success", false);
                
                if (rawRoute != null && rawRoute.contains("距离") && rawRoute.contains("时间")) {
                        try {
                                int distance = extractDistance(rawRoute);
                                int duration = extractDuration(rawRoute);
                                
                                obj.put("success", true);
                                obj.put("distance", distance);
                                obj.put("duration", duration);
                                obj.put("cost", "免费");
                                return obj;
                        } catch (Exception e) {
                                obj.put("msg", "解析步行数据失败");
                        }
                }
                obj.put("msg", "无步行路线");
                return obj;
        }
        
        private JSONObject parseTransitRoute(String rawRoute) {
                JSONObject obj = new JSONObject();
                obj.put("success", false);
                
                if (rawRoute != null && rawRoute.contains("时间") && rawRoute.contains("换乘")) {
                        try {
                                int duration = extractDuration(rawRoute);
                                int transfers = extractTransfers(rawRoute);
                                
                                obj.put("success", true);
                                obj.put("duration", duration);
                                obj.put("transfers", transfers);
                                obj.put("cost", "2-5元");
                                return obj;
                        } catch (Exception e) {
                                obj.put("msg", "解析公交数据失败");
                        }
                }
                obj.put("msg", "无公交路线");
                return obj;
        }
        
        // --- 辅助提取方法 ---
        private int extractDistance(String text) {
                // 简单的文本提取逻辑，根据你的实际情况调整
                if (text.contains("距离")) {
                        String[] parts = text.split("距离");
                        if (parts.length > 1) {
                                String numPart = parts[1].replaceAll("[^0-9.]", "");
                                if (!numPart.isEmpty()) {
                                        double km = Double.parseDouble(numPart);
                                        return (int) (km * 1000); // 转换为米
                                }
                        }
                }
                return 0;
        }
        
        private int extractDuration(String text) {
                if (text.contains("时间")) {
                        String[] parts = text.split("时间");
                        if (parts.length > 1) {
                                String numPart = parts[1].replaceAll("[^0-9]", "");
                                if (!numPart.isEmpty()) {
                                        return Integer.parseInt(numPart);
                                }
                        }
                }
                return 0;
        }
        
        private int extractTransfers(String text) {
                if (text.contains("换乘")) {
                        String[] parts = text.split("换乘");
                        if (parts.length > 1) {
                                String numPart = parts[1].replaceAll("[^0-9]", "");
                                if (!numPart.isEmpty()) {
                                        return Integer.parseInt(numPart);
                                }
                        }
                }
                return 0;
        }
        
        private String calculateTaxiFee(int distanceMeters) {
                double distanceKm = distanceMeters / 1000.0;
                double taxiFee = 8 + (distanceKm - 3) * 2;
                if (taxiFee < 8) taxiFee = 8;
                return String.format("%.0f元", taxiFee);
        }
        
        private String getWalkingRoute(String origin, String destination) {
                log.info("🚶 规划步行路线: {} -> {}", origin, destination);
                
                JSONObject routeResult = gaodeMapService.walkingRoute(origin, destination);
                log.info("📡 步行路线API返回: {}", routeResult.getString("status"));
                
                if ("1".equals(routeResult.getString("status"))) {
                        JSONObject route = routeResult.getJSONObject("route").getJSONArray("paths").getJSONObject(0);
                        String distance = route.getString("distance");
                        String duration = route.getString("duration");
                        
                        int distanceMeters = Integer.parseInt(distance);
                        int durationMinutes = Integer.parseInt(duration) / 60;
                        
                        return String.format(
                                "🚶 步行导航路线：\n📍 起点经纬度：%s\n🏁 终点经纬度：%s\n📏 距离：%d 米\n⏱️ 预计时间：%d 分钟",
                                origin, destination, distanceMeters, durationMinutes
                        );
                }
                
                return "❌ 未找到步行路线";
        }
        
        private String getDrivingRoute(String origin, String destination) {
                log.info("🚗 规划驾车路线: {} -> {}", origin, destination);
                
                JSONObject routeResult = gaodeMapService.drivingRoute(origin, destination);
                log.info("📡 驾车路线API返回: {}", routeResult.getString("status"));
                
                if ("1".equals(routeResult.getString("status"))) {
                        JSONObject route = routeResult.getJSONObject("route").getJSONArray("paths").getJSONObject(0);
                        String distance = route.getString("distance");
                        String duration = route.getString("duration");
                        
                        int distanceMeters = Integer.parseInt(distance);
                        int durationMinutes = Integer.parseInt(duration) / 60;
                        
                        double distanceKm = distanceMeters / 1000.0;
                        
                        return String.format(
                                "🚗 驾车导航路线：\n📍 起点经纬度：%s\n🏁 终点经纬度：%s\n📏 距离：%.1f 公里（%d 米）\n⏱️ 预计时间：%d 分钟",
                                origin, destination, distanceKm, distanceMeters, durationMinutes
                        );
                }
                
                return "❌ 未找到驾车路线";
        }
        
        private String getTransitRoute(String origin, String destination) {
                log.info("🚌 规划公交路线: {} -> {}", origin, destination);
                
                JSONObject routeResult = gaodeMapService.transitRoute(origin, destination, "北京");
                log.info("📡 公交路线API返回: {}", routeResult.getString("status"));
                
                if ("1".equals(routeResult.getString("status"))) {
                        JSONObject transit = routeResult.getJSONObject("route").getJSONArray("transits").getJSONObject(0);
                        String distance = transit.getString("distance");
                        String duration = transit.getString("duration");
                        
                        int distanceMeters = Integer.parseInt(distance);
                        int durationMinutes = Integer.parseInt(duration) / 60;
                        
                        StringBuilder transitInfo = new StringBuilder();
                        transitInfo.append("🚌 公交导航路线：\n");
                        transitInfo.append("📍 起点经纬度：").append(origin).append("\n");
                        transitInfo.append("🏁 终点经纬度：").append(destination).append("\n");
                        transitInfo.append("📏 距离：").append(distanceMeters).append(" 米\n");
                        transitInfo.append("⏱️ 预计时间：").append(durationMinutes).append(" 分钟\n\n");
                        transitInfo.append("🔄 换乘方案：\n");
                        
                        if (transit.containsKey("segments") && transit.getJSONArray("segments") != null) {
                                var segments = transit.getJSONArray("segments");
                                for (int i = 0; i < segments.size(); i++) {
                                        JSONObject segment = segments.getJSONObject(i);
                                        if (segment.containsKey("transit")) {
                                                var transitDetail = segment.getJSONObject("transit");
                                                if (transitDetail.containsKey("lines")) {
                                                        var lines = transitDetail.getJSONArray("lines");
                                                        for (int j = 0; j < lines.size(); j++) {
                                                                JSONObject line = lines.getJSONObject(j);
                                                                String lineName = line.getString("name");
                                                                transitInfo.append("  🚌 ").append(lineName);
                                                                
                                                                if (line.containsKey("via_stops")) {
                                                                        var viaStops = line.getJSONArray("via_stops");
                                                                        if (viaStops.size() > 0) {
                                                                                transitInfo.append(" (").append(viaStops.getJSONObject(0).getString("name"));
                                                                                if (viaStops.size() > 1) {
                                                                                        transitInfo.append(" → ").append(viaStops.getJSONObject(viaStops.size() - 1).getString("name"));
                                                                                }
                                                                                transitInfo.append(")");
                                                                        }
                                                                }
                                                                transitInfo.append("\n");
                                                        }
                                                }
                                        }
                                }
                        }
                        
                        return transitInfo.toString();
                }
                
                return "❌ 未找到公交路线";
        }
        
}
