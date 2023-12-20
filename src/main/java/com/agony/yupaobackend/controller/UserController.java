package com.agony.yupaobackend.controller;

import com.agony.yupaobackend.common.BaseResponse;
import com.agony.yupaobackend.common.ErrorCode;
import com.agony.yupaobackend.common.ResultUtils;
import com.agony.yupaobackend.exception.BusinessException;
import com.agony.yupaobackend.pojo.domain.User;
import com.agony.yupaobackend.pojo.request.UserLoginRequest;
import com.agony.yupaobackend.pojo.request.UserRegisterRequest;
import com.agony.yupaobackend.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.agony.yupaobackend.constant.UserConstant.USER_LOGIN_STATE;

/**
 * @Author Agony
 * @Create 2023/11/27 20:49
 * @Version 1.0
 */
@RestController
@RequestMapping("/user")
@CrossOrigin(value = {"http://localhost:5173/"}, allowCredentials = "true")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    @Autowired
    private RedisTemplate redisTemplate;


    @PostMapping("/register")
    public BaseResponse<Long> register(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            // return ResultUtils.error(ErrorCode.PARAMS_ERROR);
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String planetCode = userRegisterRequest.getPlanetCode();

        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            // return null;
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long result = userService.register(userAccount, userPassword, checkPassword, planetCode);
        return ResultUtils.success(result);
    }

    @PostMapping("/login")
    public BaseResponse<User> login(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            // return null;
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            // return null;
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.login(userAccount, userPassword, request);
        return ResultUtils.success(user);
    }

    @GetMapping("/search")
    public BaseResponse<List<User>> search(String username, HttpServletRequest request) {

        // 仅管理员可查询
        if (!userService.isAdmin(request)) {
            // return ResultUtils.success(new ArrayList<>());
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(username)) {
            userQueryWrapper.like("username", username);
        }
        List<User> list = userService.list(userQueryWrapper);
        List<User> users = list.stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList());
        return ResultUtils.success(users);
    }


    @PostMapping("/delete")
    public BaseResponse<Boolean> delete(long id, HttpServletRequest request) {
        // 仅管理员可查询
        if (!userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        if (id < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = userService.removeById(id);
        return ResultUtils.success(result);
    }

    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request) {
        Object o = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) o;
        if (currentUser == null) {
            // return null;
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        Long id = currentUser.getId();
        User user = userService.getById(id);
        User safetyUser = userService.getSafetyUser(user);
        return ResultUtils.success(safetyUser);
    }


    @PostMapping("/logout")
    public BaseResponse<Integer> logout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        int result = userService.logout(request);
        return ResultUtils.success(result);
    }

    @GetMapping("/search/tags")
    public BaseResponse<List<User>> searchUsersByTags(@RequestParam(required = false) List<String> tagNameList, HttpServletRequest request) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getCurrentUser(request);
        String redisKey = String.format("Agony:user:tags:%s", loginUser.getId());
        ValueOperations valueOperations = redisTemplate.opsForValue();
        // 有缓存直接读
        List<User> userList = (List<User>) valueOperations.get(redisKey);
        if (userList != null) {
            return ResultUtils.success(userList);
        }
        userList = userService.searchUsersByTags(tagNameList);
        // 写缓存，30过期
        //TODO: 不同标签搜索时间过段会有缓存
        try {
            valueOperations.set(redisKey, userList, 5000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("redis set key error ", e);
        }
        return ResultUtils.success(userList);
    }

    @PostMapping("/update")
    public BaseResponse<Integer> updateUser(@RequestBody User user, HttpServletRequest request) {
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User currentUser = userService.getCurrentUser(request);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        int result = userService.updateUser(user, currentUser);
        return ResultUtils.success(result);
    }

    /**
     * 主页推荐用户
     *
     * @param pageSize   每页查找8个
     * @param pageNumber 位于第几页
     * @param request    请求
     * @return 用户
     */
    @GetMapping("/recommend")
    public BaseResponse<Page<User>> recommendUsers(long pageSize, long pageNumber, HttpServletRequest request) {

        User loginUser = userService.getCurrentUser(request);
        String redisKey = String.format("Agony:user:recommend:%s", loginUser.getId());
        ValueOperations valueOperations = redisTemplate.opsForValue();
        // 如果有缓存，直接读缓存
        Page<User> userPage = (Page<User>) valueOperations.get(redisKey);
        if (userPage != null) {
            return ResultUtils.success(userPage);
        }
        // 无缓存，查数据库
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userPage = userService.page(new Page<>(pageNumber, pageSize), userQueryWrapper);
        // 写缓存，30过期
        try {
            valueOperations.set(redisKey, userPage, 30000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("redis set key error ", e);
        }
        return ResultUtils.success(userPage);
    }


}
