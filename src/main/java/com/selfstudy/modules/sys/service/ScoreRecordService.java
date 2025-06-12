package com.selfstudy.modules.sys.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.selfstudy.modules.sys.entity.SysScoreRecordEntity;


public interface ScoreRecordService extends IService<SysScoreRecordEntity> {
    Page<SysScoreRecordEntity> getScoreRecordById(Page<SysScoreRecordEntity> page, Long userId);
}
