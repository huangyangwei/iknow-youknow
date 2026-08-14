package com.huangyangwei.iknow.api.dto.knowledge;

import jakarta.validation.constraints.NotNull;

/**
 * 回滚请求：回滚到指定版本号，回滚后生成新版本。
 */
public class RollbackRequest {

    @NotNull(message = "versionNo 不能为空")
    private Integer versionNo;

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }
}
