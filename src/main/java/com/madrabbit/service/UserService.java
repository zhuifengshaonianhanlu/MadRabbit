package com.madrabbit.service;

import com.madrabbit.entity.User;
import com.madrabbit.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 用户服务层
 */
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    /**
     * 根据用户名查找用户
     */
    public User findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * 根据ID查找用户
     */
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * 创建用户
     */
    public int createUser(User user) {
        // 设置默认状态
        user.setStatus("ACTIVE");
        return userRepository.createUser(user);
    }

    /**
     * 更新用户
     */
    public int updateUser(User user) {
        return userRepository.updateUser(user);
    }

    /**
     * 删除用户
     */
    public int deleteUser(Long id) {
        return userRepository.deleteUser(id);
    }

    /**
     * 获取所有用户
     */
    public List<User> findAll() {
        return userRepository.findAll();
    }

    /**
     * 根据角色查找用户
     */
    public List<User> findByRole(String role) {
        return userRepository.findByRole(role);
    }

    /**
     * 更新用户密码
     */
    public int updatePassword(String username, String plainPassword, String md5Password) {
        return userRepository.updatePassword(username, plainPassword, md5Password);
    }
}