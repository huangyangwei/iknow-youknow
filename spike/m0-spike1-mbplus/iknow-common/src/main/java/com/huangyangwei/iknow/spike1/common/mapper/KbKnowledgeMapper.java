package com.huangyangwei.iknow.spike1.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huangyangwei.iknow.spike1.common.entity.KbKnowledge;

/**
 * Spike ① Mapper：位于 iknow-common 模块，由 iknow-server 的 @MapperScan 扫描。
 * 刻意不加 @Mapper 注解，以验证 @MapperScan 跨模块扫描是否生效。
 */
public interface KbKnowledgeMapper extends BaseMapper<KbKnowledge> {
}
