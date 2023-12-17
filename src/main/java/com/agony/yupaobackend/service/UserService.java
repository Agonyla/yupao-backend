package com.agony.yupaobackend.service;

import com.agony.yupaobackend.pojo.User;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * @author 11971
 * @description 针对表【user(用户)】的数据库操作Service
 * @createDate 2023-12-15 10:19:05
 */
public interface UserService extends IService<User> {
    long register(String userAccount, String userPassword, String checkPassword, String planetCode);

    User login(String userAccount, String userPassword, HttpServletRequest httpServletRequest);

    User getSafetyUser(User user);

    int logout(HttpServletRequest request);

    List<User> searchUsersByTags(List<String> tagNameList);

    /**
     * 修改用户
     *
     * @param user
     * @param loginUser
     * @return
     */
    int updateUser(User user, User loginUser);

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getCurrentUser(HttpServletRequest request);

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    boolean isAdmin(HttpServletRequest request);

    /**
     * 是否为管理员
     *
     * @param user
     * @return
     */
    boolean isAdmin(User user);
}
