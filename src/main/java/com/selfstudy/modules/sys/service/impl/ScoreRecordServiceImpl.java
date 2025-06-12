package com.selfstudy.modules.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.selfstudy.modules.sys.dao.SysScoreRecordDao;
import com.selfstudy.modules.sys.entity.SysScoreRecordEntity;
import com.selfstudy.modules.sys.service.ScoreRecordService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

@Service
public class ScoreRecordServiceImpl extends ServiceImpl<SysScoreRecordDao, SysScoreRecordEntity> implements ScoreRecordService {
    @Override
    public boolean saveBatch(Collection<SysScoreRecordEntity> entityList, int batchSize) {
        return false;
    }

    @Override
    public boolean saveOrUpdateBatch(Collection<SysScoreRecordEntity> entityList, int batchSize) {
        return false;
    }

    @Override
    public boolean updateBatchById(Collection<SysScoreRecordEntity> entityList, int batchSize) {
        return false;
    }

    @Override
    public boolean saveOrUpdate(SysScoreRecordEntity entity) {
        return false;
    }

    @Override
    public SysScoreRecordEntity getOne(Wrapper<SysScoreRecordEntity> queryWrapper, boolean throwEx) {
        return null;
    }

    @Override
    public Map<String, Object> getMap(Wrapper<SysScoreRecordEntity> queryWrapper) {
        return Collections.emptyMap();
    }

    @Override
    public <V> V getObj(Wrapper<SysScoreRecordEntity> queryWrapper, Function<? super Object, V> mapper) {
        return null;
    }



    // 分页获取用户信誉分记录
    @Override
    public Page<SysScoreRecordEntity> getScoreRecordById(Page<SysScoreRecordEntity> page, Long userId) {
        QueryWrapper<SysScoreRecordEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        return this.page(page, queryWrapper);
    }
}
