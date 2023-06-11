package com.joejoe2.chat.data.channel;

import com.joejoe2.chat.data.SliceList;
import com.joejoe2.chat.data.channel.profile.PrivateChannelProfile;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SliceOfPrivateChannel {
  private int currentPage;
  private int pageSize;
  private boolean hasNext;
  private List<PrivateChannelProfile> channels;

  public SliceOfPrivateChannel(SliceList<PrivateChannelProfile> sliceList) {
    this.currentPage = sliceList.getCurrentPage();
    this.pageSize = sliceList.getPageSize();
    this.hasNext = sliceList.isHasNext();
    this.channels = sliceList.getList();
  }
}
