package com.joejoe2.chat.service.message;

import com.joejoe2.chat.data.message.PublicMessageDto;
import com.joejoe2.chat.data.SliceList;
import com.joejoe2.chat.exception.ChannelDoesNotExist;
import com.joejoe2.chat.exception.UserDoesNotExist;
import com.joejoe2.chat.models.MessageType;
import com.joejoe2.chat.models.PublicChannel;
import com.joejoe2.chat.models.PublicMessage;
import com.joejoe2.chat.models.User;
import com.joejoe2.chat.repository.channel.PublicChannelRepository;
import com.joejoe2.chat.repository.message.PublicMessageRepository;
import com.joejoe2.chat.repository.user.UserRepository;
import com.joejoe2.chat.service.nats.NatsService;
import com.joejoe2.chat.utils.ChannelSubject;
import com.joejoe2.chat.validation.validator.MessageValidator;
import com.joejoe2.chat.validation.validator.PageRequestValidator;
import com.joejoe2.chat.validation.validator.UUIDValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;

@Service
public class PublicMessageServiceImpl implements PublicMessageService {
    @Autowired
    UserRepository userRepository;
    @Autowired
    PublicChannelRepository channelRepository;
    @Autowired
    PublicMessageRepository messageRepository;
    @Autowired
    NatsService natsService;
    @Autowired
    UUIDValidator uuidValidator;
    @Autowired
    MessageValidator messageValidator;
    @Autowired
    PageRequestValidator pageValidator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PublicMessageDto createMessage(String fromUserId, String channelId, String message) throws UserDoesNotExist, ChannelDoesNotExist {
        User user = userRepository.findById(uuidValidator.validate(fromUserId))
                .orElseThrow(() -> new UserDoesNotExist("user is not exist !"));
        PublicChannel channel = channelRepository.findById(uuidValidator.validate(channelId))
                .orElseThrow(() -> new ChannelDoesNotExist("channel is not exist !"));

        PublicMessage publicMessage = PublicMessage.builder()
                .from(user)
                .channel(channel)
                .messageType(MessageType.MESSAGE)
                .content(messageValidator.validate(message)).build();
        messageRepository.save(publicMessage);
        messageRepository.flush();
        return new PublicMessageDto(publicMessage);
    }

    @Async
    @Transactional(readOnly = true)
    @Override
    public void deliverMessage(PublicMessageDto message){
        natsService.publish(ChannelSubject.publicChannelSubject(message.getChannel().toString()), message);
    }

    @Override
    @Transactional(readOnly = true)
    public SliceList<PublicMessageDto> getAllMessages(String channelId, com.joejoe2.chat.data.PageRequest pageRequest) throws ChannelDoesNotExist {
        PageRequest paging = pageValidator.validate(pageRequest);
        PublicChannel channel = channelRepository.findById(uuidValidator.validate(channelId))
                .orElseThrow(() -> new ChannelDoesNotExist("channel is not exist !"));

        Slice<PublicMessage> slice = messageRepository.findAllByChannel(channel, paging);
        return new SliceList<>(slice.getNumber(), slice.getSize(),
                slice.getContent().stream().sorted(Comparator.comparing(PublicMessage::getUpdateAt))
                        .map(PublicMessageDto::new).toList(),
                slice.hasNext());
    }

    @Override
    @Transactional(readOnly = true)
    public SliceList<PublicMessageDto> getAllMessages(String channelId, Instant since, com.joejoe2.chat.data.PageRequest pageRequest) throws ChannelDoesNotExist {
        if (since == null) throw new IllegalArgumentException("since cannot be null !");
        PageRequest paging = pageValidator.validate(pageRequest);
        PublicChannel channel = channelRepository.findById(uuidValidator.validate(channelId))
                .orElseThrow(() -> new ChannelDoesNotExist("channel is not exist !"));

        Slice<PublicMessage> slice = messageRepository.findAllByChannelSince(channel, since, paging);
        return new SliceList<>(slice.getNumber(), slice.getSize(),
                slice.getContent().stream().sorted(Comparator.comparing(PublicMessage::getUpdateAt))
                        .map(PublicMessageDto::new).toList(),
                slice.hasNext());
    }
}
