package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.enumeration.OperationType;
import com.sky.vo.DishVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DishMapper {

    /**
     * 根据分类id查询菜品数量
     * @param categoryId
     * @return
     */
    @Select("select count(id) from dish where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);

    /**
     * 插入菜品数据
     * @param dish
     */
    @AutoFill(value = OperationType.INSERT)
    void insert(Dish dish);


    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    // DishPageQueryDTO 类里有 page,pageSize,name,categoryId,status 五个属性
    // 由于已经使用了分页插件（不是对结果分页，是拼接limit语句），因此只要把后三个条件赋值
    Page<DishVO> pageQuery(DishPageQueryDTO dishPageQueryDTO);


    /**
     * 根据主键值查询菜品
     * 用来后续判断该菜品是否在售、是否能删除
     * @param id
     * @return
     */
    @Select("select * from dish where id = #{id}")
    Dish getById(Long id);


    /**
     * 根据主键值来删除菜品数据
     * @param id
     */
    @Delete("delete from dish where id = #{id}")
    void deleteById(Long id);
}
