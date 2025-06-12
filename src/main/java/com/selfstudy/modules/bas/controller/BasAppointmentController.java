package com.selfstudy.modules.bas.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.selfstudy.common.base.PageResult;
import com.selfstudy.common.utils.R;
import com.selfstudy.config.MessageProperties;
import com.selfstudy.modules.applet.service.impl.UserServiceImpl;
import com.selfstudy.modules.applet.vo.ReservationStatsVO;
import com.selfstudy.modules.bas.dto.query.BasAppointmentQueryDTO;
import com.selfstudy.modules.bas.entity.BasAppointmentEntity;
import com.selfstudy.modules.bas.entity.BasFloorEntity;
import com.selfstudy.modules.bas.entity.BasSeatEntity;
import com.selfstudy.modules.bas.entity.BasStudyRoomEntity;
import com.selfstudy.modules.bas.service.BasAppointmentService;
import com.selfstudy.modules.bas.service.BasFloorService;
import com.selfstudy.modules.bas.service.BasSeatService;
import com.selfstudy.modules.bas.service.BasStudyRoomService;
import com.selfstudy.modules.bas.vo.CountVO;
import com.selfstudy.modules.sys.entity.SysScoreRecordEntity;
import com.selfstudy.modules.sys.service.ScoreRecordService;
import com.selfstudy.modules.user.dao.TbUserDao;
import com.selfstudy.modules.user.entity.TbUserEntity;
import io.netty.util.internal.StringUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * 预约信息表
 *
 * @author
 * @date 2025-02-01 16:46:47
 */
@RestController
@Api(tags = "预约信息管理")
@RequestMapping("/basappointment")
public class BasAppointmentController {
    @Autowired
    private BasAppointmentService basAppointmentService;
    @Autowired
    private MessageProperties messageProperties;
    @Autowired
    private BasSeatService basSeatService;
    @Autowired
    private UserServiceImpl userService;
    @Autowired
    private TbUserDao tbUserDao;
    @Autowired
    private ScoreRecordService scoreRecordService;

    @Autowired
    private BasStudyRoomService basStudyRoomService;

    @Autowired
    private BasFloorService  basFloorService;

    /**
     * 当日各自习室预约次数统计
     *
     * @param date 前端传入的日期，格式 yyyy-MM-dd
     */
    @GetMapping("/count")
    @ApiOperation("当日各自习室预约次数统计")
    public R count(
            @RequestParam("date")
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date
    ) {
        // 构造当日 00:00:00 ~ 23:59:59
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        // 分组统计：floor、room_name、COUNT(*) AS reserveCount
        QueryWrapper<BasAppointmentEntity> qw = new QueryWrapper<>();
        qw.eq("delete_flag", 0)
                .between("seat_time", start, end)
                .select("seat_id");

        List<BasAppointmentEntity> appointmentEntities = basAppointmentService.list(qw);
        if(CollectionUtil.isEmpty(appointmentEntities)){
            return R.ok();
        }


        List<Long> seatIdList = appointmentEntities.stream().map(BasAppointmentEntity::getSeatId).collect(Collectors.toList());

        List<BasSeatEntity> basSeatEntityList = basSeatService.lambdaQuery().select(BasSeatEntity::getSeatId, BasSeatEntity::getRoomId)
                .in(BasSeatEntity::getSeatId, seatIdList)
                .eq(BasSeatEntity::getDeleteFlag, "0")
                .list();
        Map<Long, Long> seatIdToRoomIdMap = basSeatEntityList.stream().collect(Collectors.toMap(BasSeatEntity::getSeatId, BasSeatEntity::getRoomId));



        Set<Long> roomIdList = basSeatEntityList.stream().map(BasSeatEntity::getRoomId).collect(Collectors.toSet());

        Map<Long, Integer> countMap = roomIdList.stream()
                .filter(Objects::nonNull) // 过滤掉 null 元素，防止统计出错
                .collect(Collectors.groupingBy(
                        Function.identity(),
                        Collectors.summingInt(e -> 1)
                ));


        List<BasStudyRoomEntity> basStudyRoomEntityList = basStudyRoomService.lambdaQuery().select(BasStudyRoomEntity::getRoomId,BasStudyRoomEntity::getFloorId, BasStudyRoomEntity::getRoomName)
                .in(BasStudyRoomEntity::getRoomId, roomIdList)
                .eq(BasStudyRoomEntity::getDeleteFlag, "0")
                .list();

        Map<Long, String> idToNameMap = basStudyRoomEntityList.stream().collect(Collectors.toMap(BasStudyRoomEntity::getRoomId, BasStudyRoomEntity::getRoomName));
        Map<Long, Long> roomIdToFloorIdMap = basStudyRoomEntityList.stream().collect(Collectors.toMap(BasStudyRoomEntity::getRoomId, BasStudyRoomEntity::getFloorId));
        List<Long> floorIdList = basStudyRoomEntityList.stream().map(BasStudyRoomEntity::getFloorId).collect(Collectors.toList());

        List<BasFloorEntity> floorEntityList = basFloorService.lambdaQuery().select(BasFloorEntity::getId, BasFloorEntity::getFloor)
                .in(BasFloorEntity::getId, floorIdList)
                .list();
        Map<Long, String> idToFloorMap = floorEntityList.stream().collect(Collectors.toMap(BasFloorEntity::getId, BasFloorEntity::getFloor));

        List<Map<String, Object>> collect1 = appointmentEntities.stream().map(item -> {
            Map<String, Object> map = new HashMap<>();
            Long seatId = item.getSeatId();
            Long roomId = seatIdToRoomIdMap.get(seatId);
            String roomName = idToNameMap.get(roomId);
            Long floorId = roomIdToFloorIdMap.get(roomId);
            String floor = idToFloorMap.get(floorId);
            map.put("floor", floor);
            map.put("roomName", roomName);
            map.put("reserveCount", countMap.get(roomId));
            return map;
        }).collect(Collectors.toList());
        collect1 = collect1.stream()
                .collect(Collectors.groupingBy(
                        map -> {
                            // 使用 floor 和 roomName 作为分组键
                            String floor = (String) map.get("floor");
                            String roomName = (String) map.get("roomName");
                            return new AbstractMap.SimpleImmutableEntry<>(floor, roomName);
                        },
                        Collectors.summingInt(map -> (Integer) map.get("reserveCount")) // 累加 count
                ))
                .entrySet().stream()
                .map(entry -> {
                    Map<String, Object> mergedMap = new HashMap<>();
                    mergedMap.put("floor", entry.getKey().getKey());
                    mergedMap.put("roomName", entry.getKey().getValue());
                    mergedMap.put("reserveCount", entry.getValue());
                    return mergedMap;
                })
                .collect(Collectors.toList());
//        List<Map<String, Object>> list = basAppointmentService.listMaps(qw);
        // 返回格式：[{ floor: "1F", room_name: "自习室A", reserveCount: 12 }, …]
        return R.ok().put("data", collect1);
    }

    /**
     * 当日座位使用情况
     */
    @GetMapping("/daily")
    @ApiOperation("当日座位使用情况")
    public R getDaily(@RequestParam(value = "date", required = true) @DateTimeFormat(pattern = "yyyy-MM-dd") Date date) {

        LocalDate localDate = Instant.ofEpochMilli(date.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        LocalDateTime start = localDate.atStartOfDay();
        LocalDateTime end = localDate.atTime(LocalTime.MAX);
        QueryWrapper<BasAppointmentEntity> query = new QueryWrapper<>();
        query.between("seat_time", start, end)
                .orderByAsc("seat_time")
                .orderByAsc("seat_id");

        List<BasAppointmentEntity> list = basAppointmentService.list(query);
        if(CollectionUtil.isEmpty(list)){
            return R.ok();
        }

        List<Long> seatIdList = list.stream().map(BasAppointmentEntity::getSeatId).collect(Collectors.toList());
        List<BasSeatEntity> basSeatEntityList = basSeatService.lambdaQuery().eq(BasSeatEntity::getDeleteFlag, "0")
                .in(BasSeatEntity::getSeatId, seatIdList).list();
        Map<Long, Integer> collect = basSeatEntityList.stream().collect(Collectors.toMap(BasSeatEntity::getSeatId, BasSeatEntity::getRoomState));

        list.stream().forEach(item->{
            item.setSeatStatus(collect.get(item.getSeatId()));
        });
        return R.ok().put("data", list);
    }

    @GetMapping("/overview")
    @ApiOperation("整体预约情况：按周/按月/按年")
    public R getOverview(@RequestParam("unit") String unit) {
        QueryWrapper<BasAppointmentEntity> qw = new QueryWrapper<>();
        qw.eq("delete_flag", 0)
                .select("DATE(seat_time) AS day", "COUNT(*) AS cnt")
                .groupBy("DATE(seat_time)")
                .orderByAsc("DATE(seat_time)");
        List<Map<String, Object>> daily = basAppointmentService.listMaps(qw);

        // 2. 按 unit 分桶
        Map<String, List<Map<String, Object>>> buckets = daily.stream()
                .collect(Collectors.groupingBy(m -> {
                    LocalDate d = ((java.sql.Date) m.get("day")).toLocalDate();
                    switch (unit) {
                        case "week":
                            WeekFields wf = WeekFields.ISO;
                            return d.getYear() + "-W" + String.format("%02d", d.get(wf.weekOfWeekBasedYear()));
                        case "year":
                            return String.valueOf(d.getYear());
                        case "month":
                        default:
                            return d.getYear() + "-" + String.format("%02d", d.getMonthValue());
                    }
                }));

        // 3. 计算指标
        List<Map<String, Object>> list = new ArrayList<>();
        for (String period : buckets.keySet()) {
            List<Map<String, Object>> grp = buckets.get(period);
            long total = grp.stream().mapToLong(m -> ((Number) m.get("cnt")).longValue()).sum();
            double avg = total * 1.0 / grp.size();
            Map<String, Object> max = grp.stream()
                    .max(Comparator.comparingLong(m -> ((Number) m.get("cnt")).longValue())).get();

            Map<String, Object> row = new HashMap<>();
            row.put("period", period);
            row.put("total", total);
            row.put("average", Math.round(avg * 100.0) / 100.0);
            row.put("maxDay", max.get("day"));
            row.put("maxCount", max.get("cnt"));
            list.add(row);
        }
        // 4. 按 period 排序
        list.sort(Comparator.comparing(m -> m.get("period").toString()));

        return R.ok().put("data", list);
    }

    @GetMapping("/timeStats")
    @ApiOperation("预约时间分布统计（小时段）")
    public R getTimeStats(
            @RequestParam("unit") String unit  // week / month / year
    ) {
        // 1. 根据 unit 计算出需要统计的日期范围
        LocalDate today = LocalDate.now();
        LocalDate startDate, endDate;
        switch (unit) {
            case "week":
                startDate = today.with(DayOfWeek.MONDAY);
                endDate = today.with(DayOfWeek.SUNDAY);
                break;
            case "month":
                startDate = today.withDayOfMonth(1);
                endDate = today.withDayOfMonth(today.lengthOfMonth());
                break;
            case "year":
                startDate = today.withDayOfYear(1);
                endDate = today.withDayOfYear(today.lengthOfYear());
                break;
            default:
                // 默认最近 7 天
                startDate = today.minusDays(6);
                endDate = today;
        }
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        // 2. 按小时聚合：返回 0…23 小时的预约次数
        QueryWrapper<BasAppointmentEntity> qw = new QueryWrapper<>();
        qw.eq("delete_flag", 0)
                .between("seat_time", start, end)
                .select("HOUR(seat_time) AS hour", "COUNT(*) AS cnt")
                .groupBy("HOUR(seat_time)")
                .orderByAsc("hour");
        List<Map<String, Object>> raw = basAppointmentService.listMaps(qw);

        // 3. 准备 0–23 小时的“满桶”，并填入查询到的值
        Map<Integer, Long> map = raw.stream()
                .collect(Collectors.toMap(
                        m -> ((Number) m.get("hour")).intValue(),
                        m -> ((Number) m.get("cnt")).longValue()
                ));
        long totalAll = map.values().stream().mapToLong(Long::longValue).sum();

        List<Map<String, Object>> result = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            long cnt = map.getOrDefault(h, 0L);
            double ratio = totalAll == 0 ? 0.0 : cnt * 1.0 / totalAll * 100;
            Map<String, Object> row = new HashMap<>();
            row.put("period", String.format("%02d:00-%02d:00", h, (h + 1) % 24));
            row.put("count", cnt);
            row.put("ratio", String.format("%.2f%%", ratio));
            row.put("trend", Collections.emptyList()); // 如果要做趋势图，可在此填充历史各 period 值
            result.add(row);
        }

        return R.ok().put("data", result);
    }


    /**
     * 统计
     */
    @GetMapping("/seatCount")
    @ApiOperation("每日预约信息统计")
    public R getDailySeatUsage(
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        // 1. 默认今天
        if (date == null) {
            date = LocalDate.now();
        }
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        // 2. 按 seat_id 分组，统计用次数
        QueryWrapper<BasAppointmentEntity> qw = new QueryWrapper<>();
        qw.eq("delete_flag", 0)
                .between("seat_time", start, end)
                .select("seat_id", "COUNT(*) AS usageCount")
                .groupBy("seat_id")
                .orderByDesc("usageCount");

        List<Map<String, Object>> raw = basAppointmentService.listMaps(qw);
        raw.stream().forEach(item->{
            Long seatId = (Long) item.get("seat_id");
            BasSeatEntity one = basSeatService.lambdaQuery().eq(BasSeatEntity::getDeleteFlag, "0")
                    .eq(BasSeatEntity::getSeatId, seatId).one();
            item.put("seat_name",one.getSeatName());
        });
        return R.ok().put("data", raw);
    }

    /**
     * 整体预约次数统计（支持年月筛选）
     *
     * @param year  年份，必传
     * @param month 月份，可选（不传或为0则按年统计）
     */
    @GetMapping("/overall")
    @ApiOperation("整体预约次数统计")
    public R countByPeriod(
            @RequestParam("unit") String unit,
            @RequestParam("year") String year,
            @RequestParam(value = "month",required = false) String month,
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate
    ) {
        // 默认最近一年或最近12个月
//        LocalDate today = LocalDate.now();
//        if (endDate == null) endDate = today;
//        if (startDate == null) {
//            startDate = unit.equals("year")
//                    ? today.minusYears(4)        // 最近5年
//                    : today.minusMonths(11);    // 最近12个月
//        }
//        LocalDateTime start = startDate.atStartOfDay();
//        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        LocalDateTime start = LocalDateTime.of(Integer.parseInt(year), 1, 1, 0, 0, 0);
//        System.out.println("Start of Year: " + startOfYear);

        // 结束时间：2025-12-31 23:59:59.999999999
        LocalDateTime end = LocalDate.of(Integer.parseInt(year), 12, 31)
                .atStartOfDay()
                .plusDays(1)
                .minusSeconds(1);

        // 根据 unit 生成 MySQL 分组／格式化表达式
        String periodExpr;
        String groupByExpr;
        if ("year".equals(unit)) {
            periodExpr = "YEAR(seat_time) AS period";
            groupByExpr = "YEAR(seat_time)";
        } else {
            // 默认 month
            periodExpr = "DATE_FORMAT(seat_time, '%Y-%m') AS period";
            groupByExpr = "DATE_FORMAT(seat_time, '%Y-%m')";
        }

        QueryWrapper<BasAppointmentEntity> qw = new QueryWrapper<>();
        qw.eq("delete_flag", 0)
                .between("seat_time", start, end)
                .select(periodExpr, "COUNT(*) AS count")
                .groupBy(groupByExpr)
                .orderByAsc("period");

        List<Map<String, Object>> list = basAppointmentService.listMaps(qw);
        if(unit.equals("year")){
            list = list.stream().filter(item-> {
                Long per = (Long) item.get("period");
                return per.equals(Long.valueOf(year));
            }).collect(Collectors.toList());
        }
        // 返回格式：[{"period":"2025-06","count":120}, {"period":"2025-07","count":98}, …]
        return R.ok().put("data", list);
    }

    /**
     * 整体预约时间分布统计（支持年月筛选）
     *
     * @param year  年份，必传
     * @param month 月份，可选（不传或为0则按年统计）
     */
    @GetMapping("/time")
    @ApiOperation("整体预约时间分布统计")
    public R getReservationTimeStats(
            @RequestParam int year,
            @RequestParam(required = false, defaultValue = "0") int month
    ) {
        List<ReservationStatsVO> stats = basAppointmentService.getReservationTimeStats(year, month);
        return R.ok().put("data", stats);
    }

    /**
     * 列表
     */
    @GetMapping("/list")
    @ApiOperation("预约信息分页列表")
    public R list(BasAppointmentQueryDTO queryDTO) {
        PageResult<BasAppointmentEntity> pageResult = basAppointmentService.queryByHelper(queryDTO);
        return R.ok().put("data", pageResult);
    }

    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    @ApiOperation("每日预约信息详情")
    public R info(@ApiParam(value = "预约id") @PathVariable("id") Long id) {
        BasAppointmentEntity basAppointment = basAppointmentService.getById(id);
        return R.ok().put("basAppointment", basAppointment);
    }


    /**
     * 签到
     */
    @PostMapping("/stateOn")
    @ApiOperation("已签到0，1未签到")
    @Transactional(rollbackFor = Exception.class)
    public R stateOn(@ApiParam(value = "预约id") Long id, Long seatId) {
        // 处理签到是否迟到逻辑
        BasAppointmentEntity basAppointment = basAppointmentService.getAppointmentById(id);
        if (basAppointment == null) {
            return R.error("预约记录不存在,无法签到");
        }
        // 10分钟判定为迟到
        Date nowDate = new Date();
        Date appointmentDate = basAppointment.getSeatTime();
        long timeDifference = nowDate.getTime() - appointmentDate.getTime();
        boolean isLate = timeDifference > 15 * 60 * 1000;
        if (isLate) {
            Long userId = basAppointment.getUserId();
            TbUserEntity tbUseruser = tbUserDao.selectById(userId);
            if (tbUseruser == null) {
                return R.error("预约用户不存在,预约失败");
            }
            // 迟到一次扣10分;
            tbUseruser.setScore(tbUseruser.getScore() - 10);
            if (tbUseruser.getScore() < 20) {
                tbUseruser.setStatus(0);
            }
            tbUserDao.updateById(tbUseruser);
            // 插入荣誉分记录表
            SysScoreRecordEntity sysScoreRecordEntity = new SysScoreRecordEntity();
            sysScoreRecordEntity.setScoreChange(-10);
            sysScoreRecordEntity.setUserId(userId);
            sysScoreRecordEntity.setAction("'迟到扣分'");
            sysScoreRecordEntity.setCreateTime(new Date());
            sysScoreRecordEntity.setUpdateTime(new Date());
            sysScoreRecordEntity.setDescription("因迟到超过 15 分钟，扣除 10 积分");
            boolean saveFlag = scoreRecordService.save(sysScoreRecordEntity);
            if (!saveFlag) {
                return R.error("预约失败");
            }
        }
        // 修改座位状态为使用中
        boolean update = basSeatService.lambdaUpdate().eq(BasSeatEntity::getSeatId, seatId).set(BasSeatEntity::getRoomState, 2).update();
        // ?FIXME 这里状态字段不太对, 逻辑有点对不上
        boolean update1 = basAppointmentService.lambdaUpdate().eq(BasAppointmentEntity::getId, id).set(BasAppointmentEntity::getSeatState, 0).update();
        if (update1 && update) {
            return R.ok();
        }
        return R.error(messageProperties.getFormUpdateError());
    }
    /**
     * 审核不通过
     */
    /*@PostMapping("/stateOff")
    @ApiOperation("审核不通过2，1未审核")
    @Transactional(rollbackFor = Exception.class)
    public R stateOff(@ApiParam(value = "预约id") Long id,Long seatId){
        boolean update = basSeatService.lambdaUpdate().eq(BasSeatEntity::getSeatId, seatId).set(BasSeatEntity::getRoomState, 0).update();

        boolean update1 = basAppointmentService.lambdaUpdate().eq(BasAppointmentEntity::getId, id).set(BasAppointmentEntity::getSeatState, 2).update();
        if (update1 && update){
            return R.ok();
        }
        return R.error(messageProperties.getFormUpdateError());
    }*/

    /**
     * 删除
     */
    @DeleteMapping("/delete")
    @ApiOperation("删除预约")
    @Transactional(rollbackFor = Exception.class)
    public R delete(@RequestBody Long[] ids) {
        List<Long> seatId = basSeatService.getSeatID(Arrays.asList(ids));
        boolean update = basSeatService.lambdaUpdate().in(BasSeatEntity::getSeatId, seatId).set(BasSeatEntity::getRoomState, 0).update();
        boolean b = basAppointmentService.removeByIds(Arrays.asList(ids));
        if (b && update) {
            return R.ok();
        }
        return R.error(messageProperties.getFormDeleteError());
    }


}
