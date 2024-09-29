package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 统计指定时间区间内的营业额数据
     * @param begin
     * @param end
     * @return
     */
    public TurnoverReportVO getTurnoverReport(LocalDate begin, LocalDate end) {
        // 集合dateList用于存放从begin到end范围内的每天的日期的集合
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);

        while (!begin.equals(end)){
            // 日期计算，计算指定日期的后一天对应的日期
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        // 存放每天的营业额
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate localDate : dateList) {
            // 查询date日期对应的营业额，是指当天“已完成”的订单的金额总额
            // LocalDate 的属性包含年月日时分秒，因此就是某一天的0:00 ~ 24:00
            // 以下语句获得的 beginTime 就是某一天的零时零分零秒
            LocalDateTime beginTime = LocalDateTime.of(localDate, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(localDate, LocalTime.MAX);

            // select sum(amount) from orders where order_time > ? and order_time < ? and status = 5
            Map map = new HashMap<>();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            turnover = turnover == null ? 0.0:turnover;
            turnoverList.add(turnover);
        }

        // 封装返回结果并返回
        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }



    /**
     * 统计指定区间内的用户统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        // 当前集合用于存放从begin到end范围内的每天的日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while(!begin.equals(end)){
            // 日期计算，计算指定日期的后一天对应的日期
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        // 存放新增用户数量 select count(id) from user where create_time > beginTime and create_time < endTime
        List<Integer> newUserList = new ArrayList<>();
        // 存放总用户数量 select count(id) from user where create_time < endTime
        List<Integer> totalUserList = new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap<>();
            map.put("end", endTime);
            // 查询总用户数量
            Integer totalUser = userMapper.countByMap(map);
            totalUserList.add(totalUser);

            map.put("begin", beginTime);
            // 查询新用户数量
            Integer newUser = userMapper.countByMap(map);
            newUserList.add(newUser);
        }

        // 封装返回结果
        return UserReportVO
                .builder()
                .dateList(StringUtils.join(dateList,","))
                .newUserList(StringUtils.join(newUserList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .build();
    }


    /**
     * 根据时间区间统计订单数量
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO getOrdersStatistics(LocalDate begin, LocalDate end) {
        // 准备日期条件
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);

        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        // 同样需要把集合类型转化为字符串类型并用逗号分隔
        String data = StringUtils.join(dateList, ",");

        // 每天订单总数集合
        List<Integer> OrderCountList = new ArrayList<>();
        // 每天有效订单总数集合
        List<Integer> validOrderCountList = new ArrayList<>();

        // 开始查询
        for (LocalDate date : dateList) {
            // 得到查询订单的起始和终止时间
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            // 查询每天的订单总数
            Integer OrderCount = getOrdersCount(beginTime,endTime,null);
            // 查询每天的完成订单数
            Integer validOrderCount = getOrdersCount(beginTime,endTime,Orders.COMPLETED);

            OrderCountList.add(OrderCount);
            validOrderCountList.add(validOrderCount);
        }
        //同样需要把集合类型转化为字符串类型并用逗号分隔
        String orderCount1 = StringUtils.join(OrderCountList, ",");//每天订单总数集合
        String validOrderCount1 = StringUtils.join(validOrderCountList, ",");//每天有效订单数集

        //计算时间区域内的总订单数量
        //Integer totalOrderCounts = orderCountList.stream().reduce(Integer::sum).get();//方式一：简写方式
        Integer totalOrderCounts = 0;
        for (Integer integer : OrderCountList) {  //方式二：普通for循环方式
            totalOrderCounts = totalOrderCounts+integer;
        }
        //计算时间区域内的总有效订单数量
        //Integer validOrderCounts = validOrderCountList.stream().reduce(Integer::sum).get();//方式一：简写方式
        Integer validOrderCounts = 0;
        for (Integer integer : validOrderCountList) { //方式二：普通for循环方式
            validOrderCounts = validOrderCounts+integer;
        }

        //4.订单完成率：  总有效订单数量/总订单数量=订单完成率
        Double orderCompletionRate = 0.0;  //订单完成率的初始值
        if(totalOrderCounts != 0){ //防止分母为0出现异常
            //总有效订单数量和总有效订单数量都是Integer类型，这里使用的是Double类型接收所以需要进行转化
            orderCompletionRate = validOrderCounts.doubleValue() / totalOrderCounts;
        }

        //构造vo对象
        return OrderReportVO.builder()
                .dateList(data)  //x轴日期数据
                .orderCountList(orderCount1) //y轴每天订单总数
                .validOrderCountList(validOrderCount1)//y轴每天有效订单总数
                .totalOrderCount(totalOrderCounts) //时间区域内总订单数
                .validOrderCount(validOrderCounts) //时间区域内总有效订单数
                .orderCompletionRate(orderCompletionRate) //订单完成率
                .build();
    }


    /**
     * 统计指定时间区间内某一状态的订单总数
     * @param beginTime
     * @param endTime
     * @param status
     * @return
     */
    private Integer getOrdersCount(LocalDateTime beginTime, LocalDateTime endTime, Integer status) {
        Map map = new HashMap();
        map.put("begin", beginTime);
        map.put("end", endTime);
        map.put("status", status);
        return orderMapper.countByMap(map);
    }


    /**
     * 查询某段时间销量前10菜品
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO top10(LocalDate begin, LocalDate end) {
        // 通过日期的起始和终止时间来查询
        List<GoodsSalesDTO> salesTop10 = orderMapper.countSaleTop10(
                LocalDateTime.of(begin,LocalTime.MIN),
                LocalDateTime.of(end,LocalTime.MAX)
        );
        if(salesTop10 == null){
            return new SalesTop10ReportVO();
        }

        // 查询得到每个菜品的名字及其销量
//        List<String> nameList = new ArrayList<>();
//        List<Integer> numberList = new ArrayList<>();
//        for (GoodsSalesDTO goodsSalesDTO : salesTop10) {
//            nameList.add(goodsSalesDTO.getName());
//            numberList.add(goodsSalesDTO.getNumber());
//        }

        List<String> names = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        String nameList = StringUtils.join(names, ",");

        List<Integer> numbers = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String numberList = StringUtils.join(numbers, ",");

        // 将销量封装到 SalesTop10ReportVO 对象里返回
//        SalesTop10ReportVO salesTop10ReportVO = new SalesTop10ReportVO();
//        salesTop10ReportVO.setNameList(StringUtils.join(nameList, ","));
//        salesTop10ReportVO.setNumberList(StringUtils.join(numberList, ","));
//        return salesTop10ReportVO;

        return SalesTop10ReportVO.builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();

    }
}
