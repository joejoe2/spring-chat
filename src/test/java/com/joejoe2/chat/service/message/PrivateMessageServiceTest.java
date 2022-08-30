package com.joejoe2.chat.service.message;

import com.joejoe2.chat.data.PageRequest;
import com.joejoe2.chat.data.channel.profile.PrivateChannelProfile;
import com.joejoe2.chat.data.message.PrivateMessageDto;
import com.joejoe2.chat.data.SliceList;
import com.joejoe2.chat.models.PrivateChannel;
import com.joejoe2.chat.models.User;
import com.joejoe2.chat.repository.channel.PrivateChannelRepository;
import com.joejoe2.chat.repository.message.PrivateMessageRepository;
import com.joejoe2.chat.repository.user.UserRepository;
import com.joejoe2.chat.service.channel.PrivateChannelService;
import com.joejoe2.chat.service.message.PrivateMessageService;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class PrivateMessageServiceTest {
    @MockBean
    Connection connection;
    @MockBean
    Dispatcher dispatcher;
    @Autowired
    PrivateChannelService channelService;
    @Autowired
    PrivateMessageService messageService;
    @Autowired
    PrivateChannelRepository channelRepository;
    @Autowired
    PrivateMessageRepository messageRepository;
    @Autowired
    UserRepository userRepository;

    User userA, userB, userC, userD;

    @BeforeEach
    void setUp() {
        userA=User.builder()
                .id(UUID.fromString("2354705e-cabf-40dd-b9c5-47a6f1bd5a2d"))
                .userName("A").build();
        userB=User.builder()
                .id(UUID.fromString("2354705e-cabf-40dd-b9c5-47a6f1bd5a3d"))
                .userName("B").build();
        userC=User.builder()
                .id(UUID.fromString("2354705e-cabf-40dd-b9c5-47a6f1bd5a4d"))
                .userName("C").build();
        userD=User.builder()
                .id(UUID.fromString("2354705e-cabf-40dd-b9c5-47a6f1bd5a5d"))
                .userName("D").build();
        userRepository.saveAll(Arrays.asList(userA, userB, userC, userD));
    }

    @AfterEach
    void tearDown() {
        channelRepository.deleteAll();
        messageRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createMessage() throws Exception {
        //prepare channel
        PrivateChannelProfile channel = channelService.createChannelBetween(userA.getId().toString(), userC.getId().toString());
        //test IllegalArgument
        assertThrows(IllegalArgumentException.class, ()->messageService.createMessage(userA.getId().toString(), "invalid_id", null));
        //test success
        PrivateMessageDto message = messageService.createMessage(userA.getId().toString(), channel.getId(), "test");
        assertTrue(messageRepository.existsById(message.getId()));
    }

    @Test
    void getAllMessages() throws Exception{
        //prepare channel
        PrivateChannelProfile channel = channelService.createChannelBetween(userA.getId().toString(), userC.getId().toString());
        //prepare messages
        List<String> messages = new LinkedList<>();
        for (int i=0;i<10;i++){
            messageService.createMessage(userA.getId().toString(), channel.getId(), "msg"+i);
            messages.add("msg"+i);
        }
        //test IllegalArgument
        assertThrows(IllegalArgumentException.class, ()->messageService.getAllMessages(userA.getId().toString(), PageRequest.builder().page(-1).size(0).build()));
        //test success
        //receive from user A
        SliceList<PrivateMessageDto> sliceList = messageService.getAllMessages(userA.getId().toString(), PageRequest.builder().page(1).size(4).build());
        assertTrue(sliceList.isHasNext());
        assertEquals(1, sliceList.getCurrentPage());
        assertEquals(4, sliceList.getPageSize());
        assertEquals(messages.subList(2, 6), sliceList.getList().stream().map(PrivateMessageDto::getContent).toList());
        sliceList = messageService.getAllMessages(userA.getId().toString(), PageRequest.builder().page(2).size(4).build());
        //receive from user C
        assertFalse(sliceList.isHasNext());
        assertEquals(2, sliceList.getCurrentPage());
        assertEquals(4, sliceList.getPageSize());
        assertEquals(messages.subList(0, 2), sliceList.getList().stream().map(PrivateMessageDto::getContent).toList());
    }

    @Test
    void getAllMessagesSince() throws Exception{
        //prepare channel
        PrivateChannelProfile channel = channelService.createChannelBetween(userA.getId().toString(), userC.getId().toString());
        //prepare messages
        for (int i=0;i<10;i++){
            messageService.createMessage(userA.getId().toString(), channel.getId(), "msg");
        }
        Instant since = Instant.now();
        List<String> messages = new LinkedList<>();
        for (int i=0;i<10;i++){
            messageService.createMessage(userA.getId().toString(), channel.getId().toString(), "msg"+i);
            messages.add("msg"+i);
        }
        //test IllegalArgument
        assertThrows(IllegalArgumentException.class, ()->messageService.getAllMessages(userA.getId().toString(), since, PageRequest.builder().page(-1).size(0).build()));
        //test success
        //receive from user A
        SliceList<PrivateMessageDto> sliceList = messageService.getAllMessages(userA.getId().toString(), since, PageRequest.builder().page(1).size(4).build());
        assertTrue(sliceList.isHasNext());
        assertEquals(1, sliceList.getCurrentPage());
        assertEquals(4, sliceList.getPageSize());
        assertEquals(messages.subList(2, 6), sliceList.getList().stream().map(PrivateMessageDto::getContent).toList());
        sliceList = messageService.getAllMessages(userA.getId().toString(), since, PageRequest.builder().page(2).size(4).build());
        //receive from user C
        assertFalse(sliceList.isHasNext());
        assertEquals(2, sliceList.getCurrentPage());
        assertEquals(4, sliceList.getPageSize());
        assertEquals(messages.subList(0, 2), sliceList.getList().stream().map(PrivateMessageDto::getContent).toList());
    }
}