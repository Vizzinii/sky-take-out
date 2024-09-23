package com.sky.controller.user;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("userShopController")
@RequestMapping("/user/shop")
@Api(tags = "客户相关接口")
@Slf4j
public class ShopController {

    @Autowired
    private RedisTemplate redisTemplate;

    @GetMapping("/status")
    @ApiOperation("查询得到店铺的营业状态")
    public Result<Integer> getStatus() {
        Integer shopStatus = (Integer)redisTemplate.opsForValue().get("SHOP_STATUS");
        log.info("查询得到店铺的营业状态:{}", shopStatus == 1 ? "营业中" : "已打烊");
        return Result.success(shopStatus);
    }

}
