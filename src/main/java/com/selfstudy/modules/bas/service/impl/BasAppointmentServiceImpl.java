package com.selfstudy.modules.bas.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.Page;
import com.selfstudy.common.base.PageResult;
import com.selfstudy.common.exception.RRException;
import com.selfstudy.common.utils.PageUtils;
import com.selfstudy.common.utils.Query;
import com.selfstudy.modules.applet.dto.save.BasAppointmentSaveDTO;
import com.selfstudy.modules.applet.vo.BasAppointmentVO;
import com.selfstudy.modules.applet.vo.ReservationStatsVO;
import com.selfstudy.modules.bas.dao.BasAppointmentDao;
import com.selfstudy.modules.bas.dao.BasSeatDao;
import com.selfstudy.modules.bas.dao.ReservationStatsDao;
import com.selfstudy.modules.bas.dto.query.BasAppointmentQueryDTO;
import com.selfstudy.modules.bas.entity.BasAppointmentEntity;
import com.selfstudy.modules.bas.entity.BasSeatEntity;
import com.selfstudy.modules.bas.service.BasAppointmentService;
import com.selfstudy.modules.bas.service.BasSeatService;
import com.selfstudy.modules.bas.vo.CountVO;
import com.selfstudy.modules.sys.entity.SysScoreRecordEntity;
import com.selfstudy.modules.sys.service.ScoreRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;


@Service("basAppointmentService")
public class BasAppointmentServiceImpl extends ServiceImpl<BasAppointmentDao, BasAppointmentEntity> implements BasAppointmentService {

    @Autowired
    private BasSeatService basSeatService;
    @Autowired
    private BasSeatDao basSeatDao;
    @Autowired
    private BasAppointmentDao basAppointmentDao;
    @Autowired
    private ReservationStatsDao reservationStatsDao;

    @Autowired
    private ScoreRecordService scoreRecordService;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<BasAppointmentEntity> page = this.page(new Query<BasAppointmentEntity>().getPage(params), new QueryWrapper<BasAppointmentEntity>());

        return new PageUtils(page);
    }


    @Override
    public PageResult<BasAppointmentEntity> queryByHelper(BasAppointmentQueryDTO queryDTO) {
        //
        Page<BasAppointmentEntity> page = queryDTO.defaultIPageHelper();
        List<BasAppointmentEntity> list = baseMapper.queryByHelper(queryDTO);
        return new PageResult<>(list, page.getTotal(), queryDTO);
    }

    @Override
    public List<CountVO> countAll(Date date) {
        return baseMapper.countAll(date);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean appointment(BasAppointmentSaveDTO saveDTO, Long userId) {
        boolean update = basSeatService.lambdaUpdate().eq(BasSeatEntity::getSeatId, saveDTO.getSeatId()).set(BasSeatEntity::getRoomState, 1).update();
        BasAppointmentEntity basAppointmentEntity = BeanUtil.copyProperties(saveDTO, BasAppointmentEntity.class);
        basAppointmentEntity.setSeatTime(new Date());
        basAppointmentEntity.setSeatState(1);
        basAppointmentEntity.setCreateUserId(userId);
        basAppointmentEntity.setUserId(userId);
        boolean save = save(basAppointmentEntity);
        if (save && update) {
            return true;
        }
        return false;
    }

    @Override
    public List<BasAppointmentVO> myAppointment(Long userId) {
        List<BasAppointmentVO> list = baseMapper.myAppointment(userId);
        return list;
    }

    @Override
    public Boolean cancel(Long id) {
        // 获取预约记录
        BasAppointmentEntity appointment = baseMapper.getAppointmentById(id);
        if (appointment == null) {
            throw new RRException("预约记录不存在");
        }

        // 检查预约状态
        if (appointment.getSeatState() == 2) {
            throw new RRException("该预约已超时未签到，无法取消");
        }

        String time = appointment.getSeatDay();
        try {
            // 解析预约开始时间
            Date startTime = parseStartTime(time);
            Date now = new Date();
            Date createTime = appointment.getCreateTime();

            // 计算可取消的截止时间
            long deadlineMillis;
            if (createTime.before(startTime)) {
                // 创建时间早于开始时间：截止到开始时间后15分钟
                deadlineMillis = startTime.getTime() + (15 * 60 * 1000);
            } else {
                // 创建时间晚于开始时间：截止到创建时间后15分钟
                deadlineMillis = createTime.getTime() + (15 * 60 * 1000);
            }

            // 检查当前时间是否超过可取消时间
            if (now.getTime() > deadlineMillis) {
                throw new RRException("已迟到，无法取消");
            }

            // 更新座位状态为可用 - 使用座位ID
            boolean updateSeatSuccess = basSeatDao.updaeSeatState(id);
            // 删除预约记录 - 使用预约ID
            boolean deleteAppointmentSuccess = removeById(id);

            if (!updateSeatSuccess || !deleteAppointmentSuccess) {
                throw new RRException("操作失败，请重试");
            }

            return true;
        } catch (RRException e) {
            throw e; // 直接抛出业务异常
        } catch (Exception e) {
            String errorMsg = String.format("取消预约时发生错误, time: %s", time);
            log.error(errorMsg, e);
            throw new RRException("系统错误，请联系管理员");
        }
    }

    // 修改返回类型为 java.util.Date
    private Date parseStartTime(String seatDay) {
        try {
            String[] parts = seatDay.split(" ");
            if (parts.length < 2) {
                throw new IllegalArgumentException("无效的时间格式");
            }

            String datePart = parts[0];
            String timeRange = parts[1];

            // 分割时间范围
            String[] timeParts = timeRange.split("-");
            if (timeParts.length < 1) {
                throw new IllegalArgumentException("缺少开始时间");
            }

            // 标准化时间格式
            String startTimeStr = normalizeTime(datePart + " " + timeParts[0]);

            // 使用 SimpleDateFormat 解析为 java.util.Date
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            return sdf.parse(startTimeStr);
        } catch (Exception e) {
            throw new RRException("时间格式解析错误: " + seatDay);
        }
    }

    // 辅助方法：标准化时间格式
    private String normalizeTime(String timeStr) {
        // 处理 "2025-05-30 8:00" -> "2025-05-30 08:00"
        return timeStr.replaceAll(" (\\d{1,2}):", " $1:").replaceAll(" (\\d):", " 0$1:");
    }

    @Override
/**
 * 处理座位预约结束操作，包括检查时间、更新座位状态和删除预约记录
 *
 * @param id 预约记录ID
 * @return 操作成功返回true，失败返回false
 * @throws RRException 如果当前时间早于预约结束时间，抛出此异常
 */ public Boolean over(Long id) {
        BasAppointmentEntity appointment = baseMapper.getAppointmentById(id);
        if (appointment == null) {
            throw new RRException("预约记录不存在");
        }

        // 检查预约状态
        if (appointment.getSeatState() == 2) { // 已迟到状态
            throw new RRException("该预约已超时未签到");
        }

        // 更新预约状态为3（已结束）
        appointment.setSeatState(3);
        boolean updateAppointment = this.updateById(appointment);

        // 更新座位状态为可用（假设0表示可用）
        boolean updateSeat = basSeatDao.updaeSeatState(id);

        return updateAppointment && updateSeat;
    }

    // 查询今日座位使用次数
    @Override
    public List<BasAppointmentEntity> selectTodaySeatUsage() {
        QueryWrapper<BasAppointmentEntity> queryWrapper = new QueryWrapper<>();
        LocalDate today = LocalDate.now();
        Date start = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date end = Date.from(today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

        queryWrapper
                .select("seat_id", "COUNT(*) AS usage_count")
                .between("seat_time", start, end)
                .groupBy("seat_id");
        return basAppointmentDao.selectList(queryWrapper);
    }

    // 年/月统计整体预约次数
    @Override
    public List<ReservationStatsVO> getReservationStats(int year, int month) {
        return reservationStatsDao.getReservationStats(year, month);
    }

    // 整体预约时间分布统计（按小时）
    @Override
    public List<ReservationStatsVO> getReservationTimeStats(int year, int month) {
        return reservationStatsDao.getReservationTimeStats(year, month);
    }

    // 时间单位内预约的时间扇形图（可选按分钟）
    @Override
    public List<ReservationStatsVO> getReservationTimeSecondStats(int year, int month, boolean byMinute) {
        return reservationStatsDao.getReservationTimeSecondStats(year, month, byMinute);
    }


    @Scheduled(cron = "0 * * * * ?") // 每分钟检查一次
    public void checkSigninStatus() {
        System.out.println("-----------------------------------------------------定时任务1执行");


        // 获取所有未开始的预约（seat_state=1）
        List<BasAppointmentEntity> appointments = baseMapper.selectList(new QueryWrapper<BasAppointmentEntity>().eq("seat_state", 1));

        DateTime now = DateUtil.date();

        for (BasAppointmentEntity app : appointments) {
            String time = app.getSeatDay();
            String[] parts = time.split(" ");
            String datePart = parts[0];
            String[] timeRange = parts[1].split("-");
            String startTimeStr = datePart + " " + timeRange[0];

            DateTime startTime = DateUtil.parse(startTimeStr, "yyyy-MM-dd HH:mm");
            DateTime latestSigninTime = DateUtil.offsetMinute(startTime, 15);

            // 如果当前时间超过开始时间15分钟且未签到
            if (now.isAfter(latestSigninTime)) {
                // 更新状态为已迟到
                app.setSeatState(2);
                updateById(app);
                // 扣除信誉分
                Long userId = app.getUserId();
                SysScoreRecordEntity entity = new SysScoreRecordEntity()
                        .setUserId(userId)
                        .setAction("迟到扣分")
                                .setScoreChange(-10)
                                        .setDescription("因迟到超过 15 分钟，扣除 10 积分")
                                                .setCreateTime(new Date())
                                                        .setUpdateTime(new Date());
                scoreRecordService.save(entity);
                // 释放座位
                basSeatDao.updaeSeatState(app.getId());
            }
        }
    }

    @Scheduled(cron = "0 * * * * ?") // 每分钟执行一次
    public void autoEndExpiredAppointments() {

        System.out.println("-----------------------------------------------------定时任务2执行");

        List<BasAppointmentEntity> expiredAppointments = baseMapper.getExpiredAppointments();

        for (BasAppointmentEntity appointment : expiredAppointments) {
            try {
                // 更新状态为3（已释放）
                appointment.setSeatState(3);
                baseMapper.updateById(appointment);
                // 释放座位
                basSeatDao.updaeSeatState(appointment.getSeatId());
            } catch (Exception e) {
                log.error("自动结束预约失败，ID：" + appointment.getId(), e);
            }
        }
    }

    @Override
    public BasAppointmentEntity getAppointmentById(Long id) {
        return baseMapper.getAppointmentById(id);
    }
}