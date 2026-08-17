package com.huangyangwei.iknow.api.dto.knowledge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 标签创建请求。
 */
public class TagRequest {

    @NotBlank(message = "标签名称不能为空")
    @Size(max = 64, message = "标签名称最长 64 字符")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
