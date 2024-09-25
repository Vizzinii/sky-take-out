package com.sky.mapper;


import com.sky.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    /**
     * 根据openid查询用户
     * @param openId
     * @return
     */
    @Select("select * from user where openid = #{openid}")
    User getUserByOpenId(String openId);

    /**
     * 向user表里插入一个新的对象
     * @param user
     */
    void insert(User user);
}
