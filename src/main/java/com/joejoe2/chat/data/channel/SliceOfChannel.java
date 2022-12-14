package com.joejoe2.chat.data.channel;

import com.joejoe2.chat.data.SliceList;
import com.joejoe2.chat.data.channel.profile.PrivateChannelProfile;
import lombok.Data;

import java.util.List;

@Data
public class SliceOfChannel {
    private int currentPage;
    private int pageSize;
    private boolean hasNext;
    private List<PrivateChannelProfile> channels;

    public SliceOfChannel(SliceList<PrivateChannelProfile> sliceList) {
        this.currentPage = sliceList.getCurrentPage();
        this.pageSize = sliceList.getPageSize();
        this.hasNext = sliceList.isHasNext();
        this.channels = sliceList.getList();
    }
}
