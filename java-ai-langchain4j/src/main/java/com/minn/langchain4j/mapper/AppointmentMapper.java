package com.minn.langchain4j.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minn.langchain4j.entity.Appointment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
//AppointmentMapper 继承MybatisPlus提供的BaseMapper
public interface AppointmentMapper extends BaseMapper<Appointment> {

}
