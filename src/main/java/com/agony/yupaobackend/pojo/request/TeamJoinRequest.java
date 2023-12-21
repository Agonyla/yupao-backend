package com.agony.yupaobackend.pojo.request;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 队伍
 */

@Data
public class TeamJoinRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 102880035341852979L;
    /**
     * id
     */
    private Long id;

    /**
     * 密码
     */
    private String password;

}