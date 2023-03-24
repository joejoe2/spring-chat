package com.joejoe2.chat.data.channel;

import com.joejoe2.chat.data.PageList;
import com.joejoe2.chat.data.channel.profile.PublicChannelProfile;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PageOfChannel {
  private long totalItems;
  private int currentPage;
  private int totalPages;
  private int pageSize;
  private List<PublicChannelProfile> channels;

  public PageOfChannel(PageList<PublicChannelProfile> pageList) {
    this.totalItems = pageList.getTotalItems();
    this.currentPage = pageList.getCurrentPage();
    this.totalPages = pageList.getTotalPages();
    this.pageSize = pageList.getPageSize();
    this.channels = pageList.getList();
  }
}
