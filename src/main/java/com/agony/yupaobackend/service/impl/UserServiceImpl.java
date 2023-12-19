package com.agony.yupaobackend.service.impl;

import com.agony.yupaobackend.common.ErrorCode;
import com.agony.yupaobackend.exception.BusinessException;
import com.agony.yupaobackend.mapper.UserMapper;
import com.agony.yupaobackend.pojo.domain.User;
import com.agony.yupaobackend.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.agony.yupaobackend.constant.UserConstant.ADMIN_ROLE;
import static com.agony.yupaobackend.constant.UserConstant.USER_LOGIN_STATE;

/**
 * @author 11971
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2023-12-15 10:19:05
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Autowired
    private UserMapper userMapper;

    private static final String SALT = "Agony";


    @Override
    public long register(String userAccount, String userPassword, String checkPassword, String planetCode) {
        // 非空校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        // 账户长度校验
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        // 密码长度校验
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        // 两次密码校验
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次密码不一致");
        }
        // 星球编号校验
        if (planetCode.length() > 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "星球编号过长");
        }
        // 账户不包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号包含特殊字符");
        }
        // 账户不能重复
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("userAccount", userAccount);
        User selectUser = userMapper.selectOne(userQueryWrapper);
        if (selectUser != null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号重复");
        }
        // 星球编号不能重复
        userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("planetCode", planetCode);
        selectUser = userMapper.selectOne(userQueryWrapper);
        if (selectUser != null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "星球编号重复");
        }
        // 密码加密
        String verifyPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes(StandardCharsets.UTF_8));
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(verifyPassword);
        user.setPlanetCode(planetCode);
        userMapper.insert(user);
        return user.getId();
    }


    @Override
    public User login(String userAccount, String userPassword, HttpServletRequest httpServletRequest) {
        // 非空校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            return null;
        }
        // 账户长度校验
        if (userAccount.length() < 4) {
            return null;
        }
        // 密码长度校验
        if (userPassword.length() < 8) {
            return null;
        }
        // 账户不包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            return null;
        }
        // 密码校验
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        String verifyPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes(StandardCharsets.UTF_8));
        userQueryWrapper.eq("userAccount", userAccount);
        userQueryWrapper.eq("userPassword", verifyPassword);
        User user = userMapper.selectOne(userQueryWrapper);
        if (user == null) {
            return null;
        }
        // 用户信息脱敏
        User safetyUser = getSafetyUser(user);

        // 记录用户的登录态
        httpServletRequest.getSession().setAttribute(USER_LOGIN_STATE, safetyUser);

        return safetyUser;
    }

    @Override
    public User getSafetyUser(User user) {

        if (user == null) {
            return null;
        }
        User safetyUser = new User();
        safetyUser.setId(user.getId());
        safetyUser.setUsername(user.getUsername());
        safetyUser.setUserAccount(user.getUserAccount());
        safetyUser.setAvatarUrl(user.getAvatarUrl());
        safetyUser.setGender(user.getGender());
        safetyUser.setUserRole(user.getUserRole());
        safetyUser.setPhone(user.getPhone());
        safetyUser.setEmail(user.getEmail());
        safetyUser.setUserStatus(user.getUserStatus());
        safetyUser.setCreateTime(user.getCreateTime());
        safetyUser.setPlanetCode(user.getPlanetCode());
        safetyUser.setTags(user.getTags());
        safetyUser.setProfile(user.getProfile());
        return safetyUser;
    }

    @Override
    public int logout(HttpServletRequest request) {
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return 1;
    }

    /**
     * 通过内存查询标签
     *
     * @param tagNameList 标签列表
     * @return 查询用户
     */
    @Override
    public List<User> searchUsersByTags(List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //1.先查询所有用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        List<User> userList = userMapper.selectList(queryWrapper);
        Gson gson = new Gson();
        //2.判断内存中是否包含要求的标签
        return userList.stream().filter(user -> {
            String tagstr = user.getTags();
            Set<String> tempTagNameSet = gson.fromJson(tagstr, new TypeToken<Set<String>>() {
            }.getType());
            //java8  Optional 来判断空
            tempTagNameSet = Optional.ofNullable(tempTagNameSet).orElse(new HashSet<>());
            for (String tagName : tagNameList) {
                if (!tempTagNameSet.contains(tagName)) {
                    return false;
                }
            }
            return true;
        }).map(this::getSafetyUser).collect(Collectors.toList());
    }

    /**
     * 修改用户信息
     *
     * @param user      前端传递的用户
     * @param loginUser 当前登录的用户
     * @return 返回更新的条数
     */
    @Override
    public int updateUser(User user, User loginUser) {
        Long id = user.getId();
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 管理员可以跟新任何用户信息
        // 非管理员只能更新自己的信息
        if (!isAdmin(loginUser) && !Objects.equals(user.getId(), loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        return userMapper.updateById(user);
    }

    /**
     * 获取当前用户
     *
     * @param request
     * @return
     */
    @Override
    public User getCurrentUser(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object o = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (o == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return (User) o;

    }

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        Object o = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) o;
        return user != null && user.getUserRole() == ADMIN_ROLE;
    }

    /**
     * 是否为管理员
     *
     * @param user
     * @return
     */
    @Override
    public boolean isAdmin(User user) {
        return user != null && user.getUserRole() == ADMIN_ROLE;
    }

    /**
     * 通过sql语句查询标签
     *
     * @param tagNameList 标签列表
     * @return 查询用户
     */
    @Deprecated
    private List<User> searchUsersByTagBySql(List<String> tagNameList) {
        // 非空校验
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // sql查询
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        for (String tagName : tagNameList) {
            userQueryWrapper.like("tags", tagName);
        }
        List<User> users = userMapper.selectList(userQueryWrapper);

        return users.stream().map(this::getSafetyUser).collect(Collectors.toList());
    }
}




