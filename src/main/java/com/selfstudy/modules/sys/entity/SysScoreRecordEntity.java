package com.selfstudy.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * 荣誉积分记录实体类
 */
@Data
@Accessors(chain = true)
@TableName("sys_score_record")  // 指定数据库表名
public class SysScoreRecordEntity {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 用户ID，外键，指向 tb_user 表
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 积分变化值（正值为增加，负值为减少）
     */

    private Integer scoreChange;

    /**
     * 积分变动的原因
     */

    private String action;

    /**
     * 积分变动的详细描述
     */

    private String description;

    /**
     * 记录创建时间
     */
    private Date createTime;

    /**
     * 记录更新时间
     */
    private Date updateTime;
}
