package com.minn.langchain4j.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.minn.langchain4j.entity.DoctorSchedule;
import com.minn.langchain4j.mapper.DoctorScheduleMapper;
import com.minn.langchain4j.service.DoctorScheduleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DoctorScheduleServiceImpl extends ServiceImpl<DoctorScheduleMapper, DoctorSchedule> implements DoctorScheduleService {
    
    @Override
    public DoctorSchedule getSchedule(String department, String date, String time, String doctorName) {
        LambdaQueryWrapper<DoctorSchedule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DoctorSchedule::getDepartment, department);
        queryWrapper.eq(DoctorSchedule::getTime, time);
        
        if (doctorName != null && !doctorName.isEmpty()) {
            queryWrapper.eq(DoctorSchedule::getDoctorName, doctorName);
        }
        
        queryWrapper.orderByAsc(DoctorSchedule::getBookedSlots);
        queryWrapper.last("LIMIT 1");
        
        return baseMapper.selectOne(queryWrapper);
    }
    
    @Override
    public boolean hasAvailableSlots(String department, String date, String time, String doctorName) {
        DoctorSchedule schedule = getSchedule(department, date, time, doctorName);
        if (schedule == null) {
            return false;
        }
        return schedule.getBookedSlots() < schedule.getTotalSlots();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bookSlot(String department, String date, String time, String doctorName) {
        LambdaQueryWrapper<DoctorSchedule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DoctorSchedule::getDepartment, department);
        queryWrapper.eq(DoctorSchedule::getTime, time);
        
        if (doctorName != null && !doctorName.isEmpty()) {
            queryWrapper.eq(DoctorSchedule::getDoctorName, doctorName);
        } else {
            queryWrapper.apply("booked_slots < total_slots");
            queryWrapper.orderByAsc(DoctorSchedule::getBookedSlots);
        }
        
        queryWrapper.last("LIMIT 1");
        
        DoctorSchedule schedule = baseMapper.selectOne(queryWrapper);
        if (schedule != null && schedule.getBookedSlots() < schedule.getTotalSlots()) {
            schedule.setBookedSlots(schedule.getBookedSlots() + 1);
            return baseMapper.updateById(schedule) > 0;
        }
        return false;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelSlot(String department, String date, String time, String doctorName) {
        LambdaQueryWrapper<DoctorSchedule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DoctorSchedule::getDepartment, department);
        queryWrapper.eq(DoctorSchedule::getTime, time);
        
        if (doctorName != null && !doctorName.isEmpty()) {
            queryWrapper.eq(DoctorSchedule::getDoctorName, doctorName);
        }
        
        queryWrapper.last("LIMIT 1");
        
        DoctorSchedule schedule = baseMapper.selectOne(queryWrapper);
        if (schedule != null && schedule.getBookedSlots() > 0) {
            schedule.setBookedSlots(schedule.getBookedSlots() - 1);
            return baseMapper.updateById(schedule) > 0;
        }
        return false;
    }
}

