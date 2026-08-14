package com.huangyangwei.iknow.spike1.server;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huangyangwei.iknow.spike1.common.entity.KbKnowledge;
import com.huangyangwei.iknow.spike1.common.mapper.KbKnowledgeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spike ① 验收测试：
 * 1. Spring Boot 4 + mybatis-plus-spring-boot4-starter 正常启动
 * 2. @MapperScan 跨模块（iknow-common）扫描生效
 * 3. CRUD（insert/select/update/delete）正常
 * 4. PaginationInnerInterceptor(DbType.POSTGRE_SQL) 分页查询正常
 */
@SpringBootTest
@Import(EmbeddedPostgresConfig.class)
class Spike1ApplicationTests {

    @Autowired
    private KbKnowledgeMapper kbKnowledgeMapper;

    @BeforeEach
    void cleanTable() {
        kbKnowledgeMapper.delete(new QueryWrapper<>());
    }

    @Test
    void mapperBeanIsScannedAcrossModules() {
        assertThat(kbKnowledgeMapper).isNotNull();
    }

    @Test
    void crudWorks() {
        KbKnowledge k1 = newKb("Spring Boot 4 迁移指南", "jakarta-only", "published");
        KbKnowledge k2 = newKb("pgvector HNSW 配置", "vector_cosine_ops", "published");
        KbKnowledge k3 = newKb("Spring AI 2.0 ChatClient", "streaming", "draft");
        KbKnowledge k4 = newKb("MyBatis-Plus 分页插件", "PaginationInnerInterceptor", "published");
        KbKnowledge k5 = newKb("Redis 两级缓存", "Caffeine", "published");
        KbKnowledge k6 = newKb("Nacos 3.x 配置中心", "spring cloud alibaba", "draft");

        assertThat(kbKnowledgeMapper.insert(k1)).isEqualTo(1);
        assertThat(kbKnowledgeMapper.insert(k2)).isEqualTo(1);
        assertThat(kbKnowledgeMapper.insert(k3)).isEqualTo(1);
        assertThat(kbKnowledgeMapper.insert(k4)).isEqualTo(1);
        assertThat(kbKnowledgeMapper.insert(k5)).isEqualTo(1);
        assertThat(kbKnowledgeMapper.insert(k6)).isEqualTo(1);

        // select by id
        KbKnowledge loaded = kbKnowledgeMapper.selectById(k1.getId());
        assertThat(loaded).isNotNull();
        assertThat(loaded.getTitle()).isEqualTo("Spring Boot 4 迁移指南");

        // update
        KbKnowledge update = new KbKnowledge();
        update.setId(k1.getId());
        update.setStatus("archived");
        assertThat(kbKnowledgeMapper.updateById(update)).isEqualTo(1);
        assertThat(kbKnowledgeMapper.selectById(k1.getId()).getStatus()).isEqualTo("archived");

        // delete
        assertThat(kbKnowledgeMapper.deleteById(k6.getId())).isEqualTo(1);
        assertThat(kbKnowledgeMapper.selectById(k6.getId())).isNull();
    }

    @Test
    void paginationWorksWithPostgreSqlDialect() {
        // 准备 25 行数据，验证分页总数与 limit/offset 行为；每 5 行 1 条 draft，验证条件分页
        for (int i = 0; i < 25; i++) {
            String status = (i % 5 == 4) ? "draft" : "published";
            kbKnowledgeMapper.insert(newKb("知识条目-" + i, "分页验证 " + i, status));
        }

        IPage<KbKnowledge> page1 = kbKnowledgeMapper.selectPage(new Page<>(1, 5), null);
        assertThat(page1.getTotal()).isEqualTo(25);
        assertThat(page1.getPages()).isEqualTo(5);
        assertThat(page1.getRecords()).hasSize(5);

        IPage<KbKnowledge> page3 = kbKnowledgeMapper.selectPage(new Page<>(3, 5), null);
        assertThat(page3.getRecords()).hasSize(5);
        assertThat(page3.getRecords().get(0).getTitle()).isEqualTo("知识条目-10");

        IPage<KbKnowledge> page5 = kbKnowledgeMapper.selectPage(new Page<>(5, 5), null);
        assertThat(page5.getRecords()).hasSize(5);
        assertThat(page5.getRecords().get(4).getTitle()).isEqualTo("知识条目-24");

        IPage<KbKnowledge> page6 = kbKnowledgeMapper.selectPage(new Page<>(6, 5), null);
        assertThat(page6.getRecords()).isEmpty();

        // 条件分页
        LambdaQueryWrapper<KbKnowledge> wrapper = new LambdaQueryWrapper<KbKnowledge>()
                .eq(KbKnowledge::getStatus, "published");
        IPage<KbKnowledge> condPage = kbKnowledgeMapper.selectPage(new Page<>(1, 10), wrapper);
        assertThat(condPage.getTotal()).isEqualTo(20);
    }

    private KbKnowledge newKb(String title, String plainText, String status) {
        KbKnowledge k = new KbKnowledge();
        k.setTitle(title);
        k.setPlainText(plainText);
        k.setStatus(status);
        k.setCreatedAt(LocalDateTime.now());
        return k;
    }
}
