package com.joejoe2.chat.data.channel;

import com.joejoe2.chat.data.SliceList;
import com.joejoe2.chat.data.channel.profile.GroupChannelProfile;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SliceOfGroupChannel {
  private int currentPage;
  private int pageSize;
  private boolean hasNext;
  private List<GroupChannelProfile> channels;

  public SliceOfGroupChannel(SliceList<GroupChannelProfile> sliceList) {
    this.currentPage = sliceList.getCurrentPage();
    this.pageSize = sliceList.getPageSize();
    this.hasNext = sliceList.isHasNext();
    this.channels = sliceList.getList();
  }
}
