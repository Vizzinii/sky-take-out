package com.sky.service.impl;


import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增菜品和对应的口味
     * @param dishDTO
     */
    // 既然涉及多个表，那么就需要添加@Transactional来保持数据一致性(原子性）
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {

        Dish dish = new Dish();

        BeanUtils.copyProperties(dishDTO, dish);


        // 向菜品表插入一条数据
        dishMapper.insert(dish);

        // 现在可以获取insert语句生成的主键值
        Long dishId = dish.getId();


        // 向口味表插入多条数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        // 口味不是非必须的，可能是0条
        if (flavors != null && flavors.size() > 0) {
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);
            });
            // 可以如下 一条一条插入，但是sql支持批量插入
            //for (DishFlavor f : flavors) {}
            dishFlavorMapper.insertBatch(flavors);

        }

    }

    /**
     * 分页查询菜品
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(),dishPageQueryDTO.getPageSize());
        // pageQuery 方法需要返回一个 DishVO 对象，使得后端返回的数据能够适应接口
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }


    /**
     * 菜品批量删除
     * @param ids
     */
    @Transactional
    public void deleteBatch(List<Long> ids) {

        // 判断菜品是否能删除,即是否存在 在售 的商品
        for (Long id : ids) {
            Dish dish = dishMapper.getById(id);

            // 如果某个菜品的状态为在售，那么这次批量删除就不能继续执行
            // 直接在这里抛出一个异常
            if(dish.getStatus() == StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }

        // 判断菜品是否能删除,即是否存在 属于套餐中 的商品
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if (setmealIds != null && setmealIds.size() > 0) { //说明当前菜品已经关联到了套餐上，不能删除
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        // 删除Dish表中的菜品数据
        for (Long id : ids) {
            dishMapper.deleteById(id);
            // 删除DishFlavor表中的关联口味数据
            // 不再单独查是否有口味数据，尝试着直接删除菜品关联的所有口味
            dishFlavorMapper.deleteByDishId(id);
        }



    }
}
