package com.huangyangwei.iknow.api.dto.knowledge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 分类创建/更新请求。
 */
public class CategoryRequest {

    private Long parentId = 0L;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 64, message = "分类名称最长 64 字符")
    private String name;

    @Size(max = 64, message = "产品线最长 64 字符")
    private String productLine;

    private Integer sort = 0;

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProductLine() {
        return productLine;
    }

    public void setProductLine(String productLine) {
        this.productLine = productLine;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }
}
