package com.minn.langchain4j.tools;

import com.minn.langchain4j.entity.Appointment;
import com.minn.langchain4j.entity.DoctorSchedule;
import com.minn.langchain4j.service.AppointmentService;
import com.minn.langchain4j.service.DoctorScheduleService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AppointmentTools {
        @Autowired
        private AppointmentService appointmentService;
        
        @Autowired
        private DoctorScheduleService doctorScheduleService;
        
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
}
