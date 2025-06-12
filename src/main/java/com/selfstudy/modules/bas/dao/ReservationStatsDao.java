package com.selfstudy.modules.bas.dao;


import com.selfstudy.modules.applet.vo.ReservationStatsVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;


@Mapper
public interface ReservationStatsDao {
    List<ReservationStatsVO> getReservationStats(int year, int month);

    List<ReservationStatsVO> getReservationTimeStats(int year, int month);

    List<ReservationStatsVO> getReservationTimeSecondStats(int year, int month, boolean byMinute);
}