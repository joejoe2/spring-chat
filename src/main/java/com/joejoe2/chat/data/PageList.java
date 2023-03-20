package com.joejoe2.chat.data;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class PageList<E> {
    private long totalItems;
    private int currentPage;
    private int totalPages;
    private int pageSize;
    private List<E> list;

    public PageList(long totalItems, int currentPage, int totalPages, int pageSize, List<E> list) {
        this.totalItems = totalItems;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.pageSize = pageSize;
        this.list = list;
    }
}
