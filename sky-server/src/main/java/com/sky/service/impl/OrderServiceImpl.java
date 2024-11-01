package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WeChatPayUtil weChatPayUtil;

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {

        // 首先要处理各种异常（地址簿为空、购物车数据为空……）
        Long addressBookId = ordersSubmitDTO.getAddressBookId();
        AddressBook addressBook = addressBookMapper.getById(addressBookId);
        if(addressBook == null) { //此时用户传来的地址为空
            // 直接抛出一个异常
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        //Long userId = BaseContext.getCurrentId();
        Long userId = addressBook.getUserId();

        ShoppingCart shoppingCart = new ShoppingCart();

        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);

        if(shoppingCartList == null || shoppingCartList.size() == 0) {
            throw new AddressBookBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // 1.向订单表插入一条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(userId);
        orderMapper.insert(orders);

        // 2.向订单明细表插入n条数据
        List<OrderDetail> orderDetailList= new ArrayList<>();
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            // 把订单每一条明细拷贝到一个 OrderDetail 对象中
            BeanUtils.copyProperties(cart, orderDetail);
            // 设置当前订单明细关联的订单
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);

        // 3，清空当前用户的购物车
        shoppingCartMapper.deleteByUserId(userId);

        // 4.封装VO并返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();
        return orderSubmitVO;
    }


    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );

        String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonceStr = RandomStringUtils.randomNumeric(32);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("nonceStr", nonceStr);
        jsonObject.put("timeStamp", timeStamp);
        jsonObject.put("package","prepaya_id=" + 12345678);
        jsonObject.put("signType", "MD5");
        jsonObject.put("paySign","okkkkk");

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }


    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumberAndUserId(outTradeNo, userId);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }


    /**
     * 用户端订单分页查询
     * @param pageNum
     * @param pageSize
     * @param status
     * @return
     */
    public PageResult pageQuery4User(int pageNum, int pageSize, Integer status) {

        // 设置分页
        PageHelper.startPage(pageNum, pageSize);

        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);

        // 分页条件查询
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> list = new ArrayList();

        if(page != null && page.getTotal() > 0) {
            for (Orders orders : page) {
                // 首先得到列表里每个订单的订单id
                Long ordersId = orders.getId();

                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(ordersId);

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetails);
                list.add(orderVO);
            }
        }
        return new PageResult(page.getTotal(), list);
    }


    /**
     * 查询订单详情
     * @param id
     * @return
     */
    @Override
    public OrderVO details(Long id) {

        // 根据id查询订单
        Orders orders = orderMapper.getById(id);

        // 在订单实体里查询得到订单明细
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        // 将订单明细返回给前端，后端传给前端是通过 OrderVO 的数据实体
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);

        return orderVO;
    }


    /**
     * 用户取消订单
     * @param id
     * @throws Exception
     */
    @Override
    public void userCancelById(Long id) throws Exception {
        // 首先根据id查询得到订单
        Orders ordersDB = orderMapper.getById(id);

        // 检查订单是否存在表内
        if(ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 如果订单已经开始派送，将不可被取消
        if (ordersDB.getStatus() > 3){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());

        // 更新订单的支付状态、状态、取消原因、取消时间
        // 这里没有写退款，因为真的懒还有点低烧
        if(ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            orders.setPayStatus(Orders.REFUND); //支付状态修改为已退款
        }
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户自行取消");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);

    }


    /**
     * 用户再来一单
     * @param id
     */
    @Override
    public void repetition(Long id) {
        Long userId = BaseContext.getCurrentId();
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();

            // 将原订单详情里面的菜品信息重新复制到购物车对象中
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());
        shoppingCartMapper.insertBatch(shoppingCartList);

    }


    /**
     * 商户端搜索订单
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {

        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        // 部分订单状态，需要额外返回订单菜品信息，将Orders转化为OrderVO
        List<OrderVO> orderVOList = getOrderVOList(page);

        return new PageResult(page.getTotal(), orderVOList);
    }

    private List<OrderVO> getOrderVOList(Page<Orders> page) {
        // 需要返回订单菜品信息，自定义OrderVO响应结果
        List<OrderVO> orderVOList = new ArrayList<>();

        // 获取当前分页的结果集
        List<Orders> ordersList = page.getResult();

        if(!CollectionUtils.isEmpty(ordersList)) {
            for (Orders orders : ordersList) {
                OrderVO orderVO = new OrderVO();

                // 将共同字段复制到OrderVO
                BeanUtils.copyProperties(orders, orderVO);

                String orderDishes = getOrderDishesStr(orders);
                orderVO.setOrderDishes(orderDishes);

                // 将继承当前orders数据的 orderVO 加入到结果集里
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    private String getOrderDishesStr(Orders orders) {
        // 查询订单菜品详情信息（订单中的菜品和数量）
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        // 将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3；）
        List<String> orderDishList = orderDetailList.stream().map(x -> {
            String orderDish = x.getName() + "*" + x.getNumber() + ";";
            return orderDish;
        }).collect(Collectors.toList());
        // 将该订单对应的所有菜品信息拼接在一起
        return String.join("", orderDishList);
    }


    /**
     * 商户端显示各个状态的订单数量
     * @return
     */
    @Override
    public OrderStatisticsVO statistics() {
        // 根据状态，分别查询出待接单、待派送、派送中的订单数量
        Integer toBeConfirmed =  orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);

        // 将查询出的数据封装到orderStatisticsVO中响应
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }


    /**
     * 商家接单
     * @param ordersConfirmDTO
     */
    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
//        Orders orders = new Orders();
//        orders.setId(ordersConfirmDTO.getId());
//        orders.setStatus(Orders.CONFIRMED);
        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();
        orderMapper.update(orders);
    }


    /**
     * 商家拒单
     * @param ordersRejectionDTO
     * @throws Exception
     */
    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        Orders ordersDB = orderMapper.getById(ordersRejectionDTO.getId());

        // 如果订单对象不存在，或者订单已经不是待接单状态，那么就不能拒单
        if(ordersDB == null || !ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 拒单需要修改支付状态、修改订单状态
        Integer payStatus = ordersDB.getPayStatus();
        if(payStatus == Orders.PAID){
            // 如果这笔订单已经支付了，那么就需要退款
            log.info("拒单到这里直接退款成功");
        }
        // 根据订单id来修改订单状态
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setPayStatus(Orders.REFUND);
        orders.setCancelReason(ordersRejectionDTO.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);

    }


    /**
     * 商家取消订单
     * @param ordersCancelDTO
     */
    @Override
    public void cancel(OrdersCancelDTO ordersCancelDTO) {
        // 首先得到要取消的订单的id
        Orders ordersDB = orderMapper.getById(ordersCancelDTO.getId());

        // 判断这个订单是否存在
        // 检查订单是否存在表内
        if(ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 如果订单已经开始派送，将不可被取消
        if (ordersDB.getStatus() > 3){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 取消订单也要退款
        // 拒单需要修改支付状态、修改订单状态
        Integer payStatus = ordersDB.getPayStatus();
        if(payStatus == Orders.PAID){
            // 如果这笔订单已经支付了，那么就需要退款
            log.info("取消订单到这里直接退款成功");
        }

        // 新建一个 Orders 对象，最后更新订单表的时候用这个新对象来更新
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setPayStatus(Orders.REFUND);
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }


    /**
     * 派送订单
     * @param id
     */
    @Override
    public void delivery(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在，并且状态为3
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态,状态转为派送中
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);

        orderMapper.update(orders);
    }


    /**
     * 完成订单
     * @param id
     */
    @Override
    public void complete(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在，并且状态为4
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态,状态转为完成
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());

        orderMapper.update(orders);
    }
}



