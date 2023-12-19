package com.agony.yupaobackend.pojo.dto;

import com.agony.yupaobackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @Author Agony
 * @Create 2023/12/19 20:27
 * @Version 1.0
 * @Description: Dto是普通实体类的扩展，Dto继承普通类，除了继承数据库类表对应的字段外，还扩展了包括一对多、多对一的关系类的字段等，主要用于多表查询、修改、新增和表的扩展。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TeamQuery extends PageRequest {
    /**
     * id
     */
    private Long id;

    /**
     * id 列表
     */
    private List<Long> idList;

    /**
     * 搜索关键词（同时对队伍名称和描述搜索）
     */
    private String searchText;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 最大人数
     */
    private Integer maxNum;

    /**
     * 创建用户id
     */
    private Long userId;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;
}
