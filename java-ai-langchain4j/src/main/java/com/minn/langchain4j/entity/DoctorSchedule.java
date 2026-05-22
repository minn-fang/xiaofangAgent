package com.minn.langchain4j.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DoctorSchedule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String department;
    private String doctorName;
    private String time;
    private Integer totalSlots; // 总号源数
    private Integer bookedSlots; // 已预约数
}
