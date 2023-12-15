package com.agony.yupaobackend.pojo.domain;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author Agony
 * @Create 2023/11/27 21:27
 * @Version 1.0
 */

@Data
public class UserRegisterRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 3191241716373120793L;

    private String userAccount;
    private String userPassword;
    private String checkPassword;
    private String planetCode;
}
