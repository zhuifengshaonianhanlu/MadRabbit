package com.madrabbit.repository;

import com.madrabbit.entity.User;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户数据访问层
 */
@Repository
public interface UserRepository {

    /**
     * 根据用户名查找用户
     */
    @Select("SELECT * FROM users WHERE username = #{username} AND status = 'ACTIVE'")
    User findByUsername(@Param("username") String username);

    /**
     * 根据ID查找用户
     */
    @Select("SELECT * FROM users WHERE id = #{id}")
    Optional<User> findById(@Param("id") Long id);

    /**
     * 创建用户
     */
    @Insert("INSERT INTO users(username, password, email, role, status, create_time, update_time) VALUES(#{username}, #{password}, #{email}, #{role}, #{status}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int createUser(User user);

    /**
     * 更新用户
     */
    @Update("UPDATE users SET username=#{username}, email=#{email}, role=#{role}, update_time=NOW() WHERE id=#{id}")
    int updateUser(User user);

    /**
     * 删除用户
     */
    @Update("UPDATE users SET status='INACTIVE', update_time=NOW() WHERE id=#{id}")
    int deleteUser(@Param("id") Long id);

    /**
     * 获取所有用户
     */
    @Select("SELECT * FROM users WHERE status = 'ACTIVE'")
    List<User> findAll();

    /**
     * 根据角色查找用户
     */
    @Select("SELECT * FROM users WHERE role = #{role} AND status = 'ACTIVE'")
    List<User> findByRole(@Param("role") String role);

    /**
     * 更新用户密码
     * @param username 用户名
     * @param plainPassword 明文密码
     * @param md5Password MD5 加密后的密码
     */
    @Update("UPDATE users SET password=#{plainPassword}, password_md5=#{md5Password}, update_time=NOW() WHERE username=#{username}")
    int updatePassword(@Param("username") String username, @Param("plainPassword") String plainPassword, @Param("md5Password") String md5Password);
}