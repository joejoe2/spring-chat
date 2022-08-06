package com.joejoe2.chat.data;

import lombok.Data;

import java.util.List;

@Data
public class SliceList<E> {
    private int currentPage;
    private int pageSize;
    private List<E> list;
    private boolean hasNext;

    public SliceList(int currentPage, int pageSize, List<E> list, boolean hasNext) {
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.list = list;
        this.hasNext = hasNext;
    }
}
