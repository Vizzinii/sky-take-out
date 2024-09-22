package com.sky.mapper;


import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    // 是根据多个菜品来查多个套餐 得到套餐id
    // 不仅是多表的多对多联合查询，而且查询的结果还是多个
    // 因此用一个 List<E> 泛型来接收查询结果

    /**
     * 根据菜品id查询对应的套餐id，在setmeal_dish表中来查询setmael_id。
     * @param dishIds
     * @return
     */
    // select setmeal_id from setmeal_dish where dish_id in (1,2,3,4)
    // <foreach collection="dishIds" item="dishId" separator="," open="(" close=")">
    List<Long> getSetmealIdsByDishIds(List<Long> dishIds);

    /**
     * 批量保存套餐和菜品的关联关系
     * @param setmealDishes
     */
    void insertBatch(List<SetmealDish> setmealDishes);

    /**
     * 根据套餐id删除套餐和菜品的关联关系
     * @param id
     */
    @Delete("delete from setmeal_dish where setmeal_id = #{id}")
    void deleteBySetmealId(Long id);

    /**
     * 根据套餐id查询套餐和菜品的关联关系
     * @param id
     * @return
     */
    @Select("select * from setmeal_dish where setmeal_id = #{setmealId}")
    List<SetmealDish> getDishBySetmealId(Long id);
}
