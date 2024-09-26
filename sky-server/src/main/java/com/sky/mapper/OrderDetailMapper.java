package com.sky.mapper;

import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OrderDetailMapper {

    /**
     * 向订单明细表中批量插入订单明细数据
     * @param orderDetails
     */

    void insertBatch(List<OrderDetail> orderDetails);
}
