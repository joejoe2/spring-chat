package com.joejoe2.chat.service.channel;

import com.joejoe2.chat.TestContext;
import com.joejoe2.chat.data.PageRequest;
import com.joejoe2.chat.data.SliceList;
import com.joejoe2.chat.data.UserPublicProfile;
import com.joejoe2.chat.data.channel.profile.PrivateChannelProfile;
import com.joejoe2.chat.exception.AlreadyExist;
import com.joejoe2.chat.exception.InvalidOperation;
import com.joejoe2.chat.models.PrivateChannel;
import com.joejoe2.chat.models.User;
import com.joejoe2.chat.repository.channel.PrivateChannelRepository;
import com.joejoe2.chat.repository.message.PrivateMessageRepository;
import com.joejoe2.chat.repository.user.UserRepository;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(TestContext.class)
class PrivateChannelServiceTest {
    @Autowired
    PrivateChannelService channelService;
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
    void createChannelBetween() {
        //test IllegalArgument
        assertThrows(IllegalArgumentException.class, ()->
                channelService.createChannelBetween("invalid_uid", "invalid_uid"));
        //test InvalidOperation, chat with self
        assertThrows(InvalidOperation.class, ()->
                channelService.createChannelBetween(userA.getId().toString(), userA.getId().toString()));
        //test success
        assertDoesNotThrow(()->{
            PrivateChannelProfile channel;
            for (User user : Arrays.asList(userB, userC, userD)) {
                channel=channelService.createChannelBetween(userA.getId().toString(), user.getId().toString());
                assertEquals(new HashSet<>(channel.getMembers()), new HashSet<>(Arrays.asList(new UserPublicProfile(userA), new UserPublicProfile(user))));
            }
            for (User user : Arrays.asList(userC, userD)) {
                channel=channelService.createChannelBetween(userB.getId().toString(), user.getId().toString());
                assertEquals(new HashSet<>(channel.getMembers()), new HashSet<>(Arrays.asList(new UserPublicProfile(userB), new UserPublicProfile(user))));
            }
            channel=channelService.createChannelBetween(userC.getId().toString(), userD.getId().toString());
            assertEquals(new HashSet<>(channel.getMembers()), new HashSet<>(Arrays.asList(new UserPublicProfile(userC), new UserPublicProfile(userD))));
        });
        //test AlreadyExist
        assertThrows(AlreadyExist.class, ()->
                channelService.createChannelBetween(userA.getId().toString(), userB.getId().toString()));
    }

    @Test
    void getAllChannels() throws Exception{
        //prepare channels
        for (User user : Arrays.asList(userB, userC, userD)) {
            channelService.createChannelBetween(userA.getId().toString(), user.getId().toString());
        }
        for (User user : Arrays.asList(userC, userD)) {
            channelService.createChannelBetween(userB.getId().toString(), user.getId().toString());
        }
        channelService.createChannelBetween(userC.getId().toString(), userD.getId().toString());
        //test IllegalArgument
        assertThrows(IllegalArgumentException.class, ()->
                channelService.getAllChannels("invalid_uid", PageRequest.builder().page(-1).size(0).build()));
        //test success
        SliceList<PrivateChannelProfile> channels = channelService.getAllChannels(userA.getId().toString(), PageRequest.builder().page(0).size(2).build());
        assertEquals(channels.getPageSize(), 2);
        assertEquals(channels.getCurrentPage(), 0);
        assertTrue(channels.isHasNext());
        //order by updateAt desc
        assertEquals(new HashSet<>(channels.getList().get(0).getMembers()), new HashSet<>(Arrays.asList(new UserPublicProfile(userA), new UserPublicProfile(userD))));
        assertEquals(new HashSet<>(channels.getList().get(1).getMembers()), new HashSet<>(Arrays.asList(new UserPublicProfile(userA), new UserPublicProfile(userC))));
        channels = channelService.getAllChannels(userB.getId().toString(), PageRequest.builder().page(2).size(1).build());
        assertEquals(channels.getPageSize(), 1);
        assertEquals(channels.getCurrentPage(), 2);
        assertFalse(channels.isHasNext());
        assertEquals(new HashSet<>(channels.getList().get(0).getMembers()), new HashSet<>(Arrays.asList(new UserPublicProfile(userA), new UserPublicProfile(userB))));
    }


    @Test
    void getChannelProfile() throws Exception{
        //prepare channels
        PrivateChannelProfile channel = channelService.createChannelBetween(userA.getId().toString(), userB.getId().toString());
        //test IllegalArgument
        assertThrows(IllegalArgumentException.class, ()-> channelService.getChannelProfile("", ""));
        //test InvalidOperation
        assertThrows(InvalidOperation.class, ()-> channelService.getChannelProfile(userC.getId().toString(), channel.getId()));
        //test success
        assertEquals(channel, channelService.getChannelProfile(userA.getId().toString(), channel.getId()));
        assertEquals(channel, channelService.getChannelProfile(userB.getId().toString(), channel.getId()));
    }
}