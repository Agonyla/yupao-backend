package com.agony.yupaobackend.pojo.request;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 队伍
 */

@Data
public class TeamDeleteRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = -3434800743489304366L;
    /**
     * id
     */
    private Long teamId;


}