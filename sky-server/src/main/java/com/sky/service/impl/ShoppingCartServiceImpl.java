package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.result.Result;
import com.sky.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 添加购物车方法
     *
     * @param shoppingCartDTO
     */
    @Override
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {

        //ShoppingCart cart = new ShoppingCart();

        // 首先判断购物车内是否已经有了这个商品。
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart); //被拷的在前面，目的地在后面

        // 通过Jwt令牌，拿到当前登陆的微信用户的id
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);

        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        // 如果购物车内已经有了当前商品，那么接下来的操作是update ，将数量+1即可
        if (list != null && list.size() > 0) {
            shoppingCart = list.get(0);
            shoppingCart.setNumber(shoppingCart.getNumber() + 1);
            shoppingCartMapper.updateNumberById(shoppingCart);

        } else {
            // 判断这个新增的是菜品还是套餐，然后查对应的菜品表or套餐表，从而得到新增的购物车栏目的数据
            Long dishId = shoppingCartDTO.getDishId();
            Long setmealId = shoppingCartDTO.getSetmealId();
            if (dishId != null) {
                // 本次添加到购物车的是dish菜品
                Dish dish = dishMapper.getById(dishId);
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());
            } else {
                // 本次添加到购物车的是setmeal套餐
                Setmeal setmeal = setmealMapper.getById(shoppingCartDTO.getSetmealId());
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
            }
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartMapper.insert(shoppingCart);
        }

    }


    /**
     * 查看购物车内的商品
     *
     * @return
     */
    @Override
    public List<ShoppingCart> showShoppingCart() {
/*        // 得到当前的用户id
        Long userId = BaseContext.getCurrentId();

        // 根据当前的用户id得到当前用户购物车内的数据
        // 首先构造一个 ShoppingCart 对象
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(userId)
                .build();


        // 按照用户id来查询用户的购物车数据
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        return list;*/


        return shoppingCartMapper.list(ShoppingCart.
                builder().
                userId(BaseContext.getCurrentId()).
                build());
    }


    /**
     * 清空购物车
     */
    public void cleanShoppingCart() {
        shoppingCartMapper.deleteByUserId(BaseContext.getCurrentId());
    }


    /**
     * 删除购物车内一个商品
     * @param shoppingCartDTO
     */
    @Override
    public void subShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);

        // 通过Jwt令牌，拿到当前登陆的微信用户的id
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);

        // 以菜品or套餐id加上用户id来得到购物车内这一类型对象的集合
        // 比方说点了两碗饭，那么 list 内的每个ShoppingCart的用户id都是我，菜品id都是饭
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);

        if (list != null && list.size() > 0) {
            shoppingCart = list.get(0);

            // 查询得到此商品在购物车内有几份
            Integer number = shoppingCart.getNumber();

            if(number == 1)
            {
                // 购物车内此商品仅有一份，那么直接删除
                shoppingCartMapper.deleteById(shoppingCart.getId());
            }else {
                shoppingCart.setNumber(number - 1);
                shoppingCartMapper.updateNumberById(shoppingCart);
            }
        }
    }
}
