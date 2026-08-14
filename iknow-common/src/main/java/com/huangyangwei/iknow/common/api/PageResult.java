package com.huangyangwei.iknow.common.api;

import java.io.Serializable;
import java.util.List;

/**
 * 通用分页结果：total 总条数、page 当前页、size 每页条数、pages 总页数、records 当前页数据。
 */
public class PageResult<T> implements Serializable {

    private long total;
    private long page;
    private long size;
    private long pages;
    private List<T> records;

    public PageResult() {
    }

    public PageResult(long total, long page, long size, long pages, List<T> records) {
        this.total = total;
        this.page = page;
        this.size = size;
        this.pages = pages;
        this.records = records;
    }

    public static <T> PageResult<T> of(long total, long page, long size, long pages, List<T> records) {
        return new PageResult<>(total, page, size, pages, records);
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getPage() {
        return page;
    }

    public void setPage(long page) {
        this.page = page;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getPages() {
        return pages;
    }

    public void setPages(long pages) {
        this.pages = pages;
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }
}
