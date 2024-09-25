package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 菜品管理
 */
@RestController
@RequestMapping("/admin/dish")
@Api(tags = "菜品相关接口")
@Slf4j
public class DishController {

    @Autowired
    private DishService dishService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增菜品
     * @param dishDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增菜品")
    // 加一个 @RequestBody 注解才能封装jackson格式的数据
    public Result<Object> save(@RequestBody DishDTO dishDTO) {
        log.info("新增菜品：{}", dishDTO);
        dishService.saveWithFlavor(dishDTO);

        // 得到当前新增菜品所属的分类的分类id，根据这个分类id来查询并删除redis数据
        String key = "dish_" + dishDTO.getCategoryId();

        // 每次新增菜品，都要清理redis缓存，等待前端查询时重新写一遍redis
        redisTemplate.delete(key);
        cleanCache(key);

        return Result.success();

    }


    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    // 分页查询返回的都是 PageResult 对象
    @GetMapping("/page")
    @ApiOperation("菜品分页查询")
    public Result<PageResult> page (DishPageQueryDTO dishPageQueryDTO) {
        log.info("菜品分页查询:{}", dishPageQueryDTO);
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }


    /**
     * 菜品批量删除
     */
    @DeleteMapping
    @ApiOperation("菜品批量删除")
    public Result delete(@RequestParam List<Long> ids) {
        log.info("菜品批量删除：{}",ids);
        dishService.deleteBatch(ids);

        // 将所有的菜品缓存数据都清理掉
        // 首先得到所有以 dish_ 开头的key的集合，之后直接根据集合删除掉所有相关数据
        Set keys = redisTemplate.keys("dish_*");
        redisTemplate.delete(keys);

        cleanCache("dish_*");

        return Result.success();
    }


    /**
     * 根据id查询菜品
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id来查询菜品")
    public Result<DishVO> getById(@PathVariable Long id) {
        log.info("根据id查询菜品:{}",id);
        DishVO dishVO = dishService.getByIdWithFlavor(id); //根据id查询菜品和口味
        return Result.success(dishVO);
    }


    /**
     * 修改菜品信息
     * @param dishDTO
     * @return
     */
    // 新增和修改的数据格式是一样的 用DishDTO来接收
    @PutMapping
    @ApiOperation("修改菜品信息")
    public Result update(@RequestBody DishDTO dishDTO) {
        log.info("修改菜品信息:{}", dishDTO);
        dishService.updateWithFlavor(dishDTO);

        // 将所有的菜品缓存数据都清理掉
        // 首先得到所有以 dish_ 开头的key的集合，之后直接根据集合删除掉所有相关数据
        Set keys = redisTemplate.keys("dish_*");
        redisTemplate.delete(keys);

        cleanCache("dish_*");

        return Result.success();
    }


    /**
     * 根据分类id来查询菜品
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id来查询菜品")
    public Result<List<Dish>> list(Long categoryId) {
        List<Dish> list = dishService.list(categoryId);
        return Result.success(list);
    }


    /**
     * 菜品起售停售
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("菜品起售停售")
    public Result<String> startOrStop(@PathVariable Integer status, Long id){
        dishService.startOrStop(status,id);

/*        String key = "dish_" + dishService.getByIdWithFlavor(id).getCategoryId();
        redisTemplate.delete(key);*/

        cleanCache("dish_*");

        return Result.success();
    }


    private void cleanCache(String pattern) {
        Set keys = redisTemplate.keys(pattern);
        redisTemplate.delete(keys);
    }


}
