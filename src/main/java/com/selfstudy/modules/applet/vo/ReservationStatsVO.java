package com.selfstudy.modules.applet.vo;

import lombok.Data;

@Data
public class ReservationStatsVO {
    private Integer year;
    private Integer month;
    private Integer day;
    private Integer hour;
    private Integer minute;
    private Long seatId;
    private Integer reservationCount;
    private Integer usageCount;
}