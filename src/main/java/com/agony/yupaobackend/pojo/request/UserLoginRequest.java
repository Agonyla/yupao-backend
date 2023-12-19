package com.agony.yupaobackend.pojo.request;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author Agony
 * @Create 2023/11/28 10:40
 * @Version 1.0
 */
@Data
public class UserLoginRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 3191241216373120793L;

    private String userAccount;
    private String userPassword;


}
