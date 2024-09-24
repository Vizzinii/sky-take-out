package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private DishMapper dishMapper;


    public void saveWithDish(SetmealDTO setmealDTO) {


        // 把接收到的前端传来的 setmealDTO 的属性复制给一个 setmeal 对象
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);

        // 向套餐表中插入一条数据
        setmealMapper.insert(setmeal);

        // 获得生成的套餐ID
        Long setmealId = setmeal.getId();

        // 获得套餐中的菜品们，并插入到菜品表中
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(setmealId);
        });

        // 保存套餐和菜品的关系
        setmealDishMapper.insertBatch(setmealDishes);

    }


    /**
     * 分页查询套餐
     * @param setmealPageQueryDTO
     * @return
     */
    @Transactional
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(),setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }


    /**
     * 批量删除套餐
     * @param ids
     */
    @Transactional
    public void deleteBatch(List<Long> ids) {
        // 首先判断套餐能否删除，即是否存在“在售”的套餐
        ids.forEach(id -> {
            Setmeal setmeal = setmealMapper.getById(id);
            if(setmeal.getStatus() == StatusConstant.ENABLE){
                // “在售”的套餐不能被删除
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        });

        // 已经判断出套餐可以被删除了
        ids.forEach(id -> {
            // 删除套餐表setmeal中的数据
            setmealMapper.deleteById(id);
            // 删除套餐菜品关系表中的数据
            setmealDishMapper.deleteBySetmealId(id);
        });
    }


    /**
     * 根据id查询套餐和套餐菜品关系
     * @param id
     * @return
     */
    @Transactional
    public SetmealVO getByIdWithDish(Long id) {
        // 根据id查询套餐数据
        Setmeal setmeal = setmealMapper.getById(id);

        // 根据id查询套餐相关的菜品数据
        List<SetmealDish> setmealDishes = setmealDishMapper.getDishBySetmealId(id);

        // 将查询到的数据封装进VO
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);
        setmealVO.setSetmealDishes(setmealDishes);

        return setmealVO;
    }


    /**
     * 修改套餐内数据
     * @param setmealDTO
     */
    @Transactional
    public void updateWithDish(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);

        // 1. 修改套餐表，执行update
        setmealMapper.update(setmeal);

        // 2. 删除原来套餐与菜品的关联关系,操作setmeal_dish表，执行delete
        setmealDishMapper.deleteBySetmealId(setmealDTO.getId());

        // 3. 重新编写套餐与菜品的关联关系
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(setmealDTO.getId());
        });

        // 4. 重新插入套餐与菜品的关联关系，操作setmeal_dish表，执行insert
        setmealDishMapper.insertBatch(setmealDishes);
    }


    /**
     * 启售停售套餐
     * @param status
     * @param id
     */
    @Transactional
    @Override
    public void startOrStop(Integer status, Long id) {
        // 想要启售套餐时，先检查套餐内是否有停售菜品，有则返回"套餐内包含未启售菜品，无法启售"
        if(status == StatusConstant.ENABLE){
            //select a.* from dish a left join setmeal_dish b on a.id = b.dish_id where b.setmeal_id = ?
            List<SetmealDish> setmealDishList = setmealDishMapper.getDishBySetmealId(id);

            List<Dish> dishList = dishMapper.getBySetmealId(id);
            if(dishList != null && dishList.size() > 0){
                dishList.forEach(dish -> {
                    if(dish.getStatus() == StatusConstant.DISABLE){
                        throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                    }
                });
            }

/*            if(!setmealDishList.isEmpty()){
                setmealDishList.forEach(setmealDish -> {

                    // 得到套餐内每个菜品的id
                    Long dishId = setmealDish.getId();

                    // 根据从 setmeal_dish 表内得到的菜品id，查询得到菜品
                    Dish dish = dishMapper.getById(dishId);

                    // 如果套餐的菜品队列中，有某个菜品是停售的状态，那么抛出“存在着停售菜品，无法启售套餐”的中断
                    if(dish.getStatus() == StatusConstant.DISABLE){
                        throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                    }
                });
            }*/
        }

        Setmeal setmeal = Setmeal.builder()
                .id(id)
                .status(status)
                .build();
        setmealMapper.update(setmeal);

    }


    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }


    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {

        return setmealMapper.getDishItemBySetmealId(id);
    }

}
