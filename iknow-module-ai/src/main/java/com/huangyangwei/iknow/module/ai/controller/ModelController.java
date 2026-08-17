package com.huangyangwei.iknow.module.ai.controller;

import com.huangyangwei.iknow.common.api.Result;
import com.huangyangwei.iknow.module.ai.model.ModelInfo;
import com.huangyangwei.iknow.module.ai.model.ModelRouter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 可用模型列表（技术方案 §7.2：GET /api/models，供前端选择器）。
 */
@RestController
@RequestMapping("/api/models")
public class ModelController {

    private final ModelRouter modelRouter;

    public ModelController(ModelRouter modelRouter) {
        this.modelRouter = modelRouter;
    }

    @GetMapping
    public Result<List<ModelInfo>> list() {
        return Result.ok(modelRouter.availableModels());
    }
}
