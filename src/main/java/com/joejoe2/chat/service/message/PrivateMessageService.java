package com.joejoe2.chat.service.message;

import com.joejoe2.chat.data.PageRequest;
import com.joejoe2.chat.data.message.PrivateMessageDto;
import com.joejoe2.chat.data.SliceList;
import com.joejoe2.chat.exception.ChannelDoesNotExist;
import com.joejoe2.chat.exception.InvalidOperation;
import com.joejoe2.chat.exception.UserDoesNotExist;

import java.time.Instant;

public interface PrivateMessageService {
    PrivateMessageDto createMessage(String fromUserId, String channelId, String message) throws UserDoesNotExist, ChannelDoesNotExist, InvalidOperation;

    void deliverMessage(PrivateMessageDto message);

    SliceList<PrivateMessageDto> getAllMessages(String userId, PageRequest pageRequest) throws UserDoesNotExist;

    SliceList<PrivateMessageDto> getAllMessages(String userId, String channelId, PageRequest pageRequest) throws UserDoesNotExist, ChannelDoesNotExist, InvalidOperation;

    SliceList<PrivateMessageDto> getAllMessages(String userId, Instant since, PageRequest pageRequest) throws UserDoesNotExist;

    SliceList<PrivateMessageDto> getAllMessages(String userId, String channelId, Instant since, PageRequest pageRequest) throws UserDoesNotExist, ChannelDoesNotExist, InvalidOperation;
}
