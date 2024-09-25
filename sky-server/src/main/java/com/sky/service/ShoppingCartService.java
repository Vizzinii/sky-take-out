package com.sky.service;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;

import java.util.List;

public interface ShoppingCartService {


    /**
     * 添加购物车的业务方法
     * @param shoppingCartDTO
     */
    void addShoppingCart(ShoppingCartDTO shoppingCartDTO);


    /**
     * 查看购物车内商品的业务方法
     * @return
     */
    List<ShoppingCart> showShoppingCart();


    /**
     * 清空购物车的业务方法
     */
    void cleanShoppingCart();


    /**
     * 删除购物车内一个商品的业务方法
     * @param shoppingCartDTO
     */
     void subShoppingCart(ShoppingCartDTO shoppingCartDTO);

}
