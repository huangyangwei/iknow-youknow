package com.huangyangwei.iknow.api.dto.feedback;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 处理反馈请求：目标状态 processing/resolved + 处理说明。
 */
public class FeedbackHandleRequest {

    @NotBlank(message = "目标状态不能为空")
    private String status;

    @Size(max = 500, message = "处理说明过长")
    private String handleNote;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getHandleNote() {
        return handleNote;
    }

    public void setHandleNote(String handleNote) {
        this.handleNote = handleNote;
    }
}
