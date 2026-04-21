package com.bitian.db.mybatis_helper.meta;

import java.util.Collections;
import java.util.List;

/**
 * 分页查询结果封装类。
 *
 * @param <T> 记录类型
 */
public class PageResult<T> {

    /**
     * 当前页数据列表
     */
    private List<T> list;

    /**
     * 总记录数
     */
    private long total;

    /**
     * 当前页码（从 1 开始）
     */
    private int pageNumber;

    /**
     * 每页条数
     */
    private int pageSize;

    /**
     * 总页数
     */
    private int pages;

    public PageResult() {
    }

    public PageResult(List<T> list, long total, int pageNumber, int pageSize) {
        this.list = list != null ? list : Collections.<T>emptyList();
        this.total = total;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.pages = pageSize > 0 ? (int) Math.ceil((double) total / pageSize) : 0;
    }

    /**
     * 是否有下一页
     */
    public boolean hasNext() {
        return pageNumber < pages;
    }

    /**
     * 是否有上一页
     */
    public boolean hasPrevious() {
        return pageNumber > 1;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getPages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }

    @Override
    public String toString() {
        return "PageResult{" +
                "pageNumber=" + pageNumber +
                ", pageSize=" + pageSize +
                ", total=" + total +
                ", totalPages=" + pages +
                ", records=" + (list != null ? list.size() : 0) + " items" +
                '}';
    }
}
