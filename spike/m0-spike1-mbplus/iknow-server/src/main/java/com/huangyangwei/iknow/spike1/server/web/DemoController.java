package com.huangyangwei.iknow.spike1.server.web;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huangyangwei.iknow.spike1.common.entity.KbKnowledge;
import com.huangyangwei.iknow.spike1.common.mapper.KbKnowledgeMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Spike ① REST 演示：MyBatis-Plus 分页查询 + 插入。
 */
@RestController
@RequestMapping("/api/kb")
public class DemoController {

    private final KbKnowledgeMapper kbKnowledgeMapper;

    public DemoController(KbKnowledgeMapper kbKnowledgeMapper) {
        this.kbKnowledgeMapper = kbKnowledgeMapper;
    }

    @PostMapping
    public KbKnowledge create(@RequestBody KbKnowledge knowledge) {
        kbKnowledgeMapper.insert(knowledge);
        return knowledge;
    }

    @GetMapping
    public IPage<KbKnowledge> page(@RequestParam(defaultValue = "1") long page,
                                   @RequestParam(defaultValue = "5") long size) {
        return kbKnowledgeMapper.selectPage(new Page<>(page, size), null);
    }
}
