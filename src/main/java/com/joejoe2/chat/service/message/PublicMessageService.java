package com.joejoe2.chat.service.message;

import com.joejoe2.chat.data.PageRequest;
import com.joejoe2.chat.data.SliceList;
import com.joejoe2.chat.data.message.PublicMessageDto;
import com.joejoe2.chat.exception.ChannelDoesNotExist;
import com.joejoe2.chat.exception.UserDoesNotExist;
import java.time.Instant;

public interface PublicMessageService {
  PublicMessageDto createMessage(String fromUserId, String channelId, String message)
      throws UserDoesNotExist, ChannelDoesNotExist;

  void deliverMessage(PublicMessageDto message);

  SliceList<PublicMessageDto> getAllMessages(String channelId, PageRequest pageRequest)
      throws ChannelDoesNotExist;

  SliceList<PublicMessageDto> getAllMessages(
      String channelId, Instant since, PageRequest pageRequest) throws ChannelDoesNotExist;
}
