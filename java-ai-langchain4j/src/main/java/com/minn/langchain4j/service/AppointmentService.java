package com.minn.langchain4j.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.minn.langchain4j.entity.Appointment;

//IService是MyBatisPlus提供的一个接口，继承了IService接口的类，就可以使用MyBatisPlus提供的方法
public interface AppointmentService extends IService<Appointment> {
        Appointment getOne(Appointment appointment);
}
