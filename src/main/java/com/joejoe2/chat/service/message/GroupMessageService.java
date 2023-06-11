package com.joejoe2.chat.service.message;

import com.joejoe2.chat.data.PageRequest;
import com.joejoe2.chat.data.SliceList;
import com.joejoe2.chat.data.message.GroupMessageDto;
import com.joejoe2.chat.exception.ChannelDoesNotExist;
import com.joejoe2.chat.exception.InvalidOperation;
import com.joejoe2.chat.exception.UserDoesNotExist;
import java.time.Instant;
import org.springframework.transaction.annotation.Transactional;

public interface GroupMessageService {
  GroupMessageDto createMessage(String fromUserId, String channelId, String message)
      throws UserDoesNotExist, ChannelDoesNotExist, InvalidOperation;

  void deliverMessage(GroupMessageDto message);

  SliceList<GroupMessageDto> getAllMessages(
      String userId, String channelId, Instant since, PageRequest pageRequest)
      throws UserDoesNotExist, ChannelDoesNotExist, InvalidOperation;

  @Transactional(readOnly = true)
  SliceList<GroupMessageDto> getInvitations(String userId, Instant since, PageRequest pageRequest)
      throws UserDoesNotExist;
}
