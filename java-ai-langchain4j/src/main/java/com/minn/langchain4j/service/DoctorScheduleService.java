package com.minn.langchain4j.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.minn.langchain4j.entity.DoctorSchedule;

public interface DoctorScheduleService extends IService<DoctorSchedule> {
    DoctorSchedule getSchedule(String department, String date, String time, String doctorName);
    boolean hasAvailableSlots(String department, String date, String time, String doctorName);
    boolean bookSlot(String department, String date, String time, String doctorName);
    boolean cancelSlot(String department, String date, String time, String doctorName);
}
