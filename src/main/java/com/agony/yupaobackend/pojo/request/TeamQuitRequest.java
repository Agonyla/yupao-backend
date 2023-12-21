package com.agony.yupaobackend.pojo.request;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 队伍
 */

@Data
public class TeamQuitRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -3903981125039880812L;
    /**
     * id
     */
    private Long teamId;


}