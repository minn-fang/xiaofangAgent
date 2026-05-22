package com.minn.langchain4j.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minn.langchain4j.entity.DoctorSchedule;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DoctorScheduleMapper extends BaseMapper<DoctorSchedule> {
}
