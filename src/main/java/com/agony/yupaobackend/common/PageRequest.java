package com.agony.yupaobackend.common;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author Agony
 * @Create 2023/12/19 14:20
 * @Version 1.0
 */
public class PageRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 679074698898469424L;

    /**
     * 一页10个
     */
    protected int pageSize = 10;
    /**
     * 第一页
     */
    protected int pageNum = 1;
}
