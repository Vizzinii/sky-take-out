package com.sky.mapper;

import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderDetailMapper {

    /**
     * 向订单明细表中批量插入订单明细数据
     * @param orderDetails
     */

    void insertBatch(List<OrderDetail> orderDetails);


    /**
     * 根据订单id查询订单明细
     * @param ordersId
     * @return
     */
    @Select("select * from order_detail where order_id = #{orderId}")
    List<OrderDetail> getByOrderId(Long ordersId);

}
