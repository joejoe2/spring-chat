package com.joejoe2.chat.service.channel;

import com.joejoe2.chat.TestContext;
import com.joejoe2.chat.data.PageRequest;
import com.joejoe2.chat.data.channel.profile.PublicChannelProfile;
import com.joejoe2.chat.exception.AlreadyExist;
import com.joejoe2.chat.exception.ChannelDoesNotExist;
import com.joejoe2.chat.models.PublicChannel;
import com.joejoe2.chat.models.User;
import com.joejoe2.chat.repository.channel.PublicChannelRepository;
import com.joejoe2.chat.repository.message.PublicMessageRepository;
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
class PublicChannelServiceTest {
    @Autowired
    PublicChannelService channelService;
    @Autowired
    PublicChannelRepository channelRepository;
    @Autowired
    PublicMessageRepository messageRepository;
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
        messageRepository.deleteAll();
        channelRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createChannel(){
        //test IllegalArgument
        assertThrows(IllegalArgumentException.class, ()->channelService.createChannel("invalid_name"));
        //test success
        assertDoesNotThrow(()->{
            PublicChannelProfile channel = channelService.createChannel("test");
            assertEquals("test", channel.getName());
        });
        //test AlreadyExist
        assertThrows(AlreadyExist.class, ()->channelService.createChannel("test"));
    }

    @Test
    void getAllChannelsWithPage() throws Exception{
        //prepare channels
        List<PublicChannelProfile> channels=new LinkedList<>();
        for (int i=0;i<50;i++){
            channels.add(channelService.createChannel("test"+i));
        }
        channels.sort(Comparator.comparing(PublicChannelProfile::getName));
        //test IllegalArgument
        assertThrows(IllegalArgumentException.class, ()->channelService.getAllChannels(PageRequest.builder().page(-1).size(0).build()));
        //test success
        assertEquals(channels.subList(10, 15), channelService.getAllChannels(PageRequest.builder().page(2).size(5).build()).getList());
    }

    @Test
    void getChannelProfile() throws Exception{
        //prepare channels
        PublicChannelProfile channel = channelService.createChannel("test");
        //test IllegalArgument
        assertThrows(IllegalArgumentException.class, ()->channelService.getChannelProfile("invalid id"));
        //test ChannelDoesNotExist
        String id=UUID.randomUUID().toString();
        while (id.equals(channel.getId())) id = UUID.randomUUID().toString();
        String finalId = id;
        assertThrows(ChannelDoesNotExist.class, ()->channelService.getChannelProfile(finalId));
        //test success
        assertEquals(channel, channelService.getChannelProfile(channel.getId().toString()));
    }
}