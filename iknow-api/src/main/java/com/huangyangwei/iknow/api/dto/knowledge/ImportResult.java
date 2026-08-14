package com.huangyangwei.iknow.api.dto.knowledge;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量导入结果：成功数、失败明细。
 */
public class ImportResult {

    private int totalCount;
    private int successCount;
    private List<ImportFailure> failures = new ArrayList<>();

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public List<ImportFailure> getFailures() {
        return failures;
    }

    public void setFailures(List<ImportFailure> failures) {
        this.failures = failures;
    }

    public static class ImportFailure {

        private String fileName;
        private String message;

        public ImportFailure() {
        }

        public ImportFailure(String fileName, String message) {
            this.fileName = fileName;
            this.message = message;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
