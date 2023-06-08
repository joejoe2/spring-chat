package com.joejoe2.chat.service.message;

import static org.junit.jupiter.api.Assertions.*;

import com.joejoe2.chat.TestContext;
import com.joejoe2.chat.data.PageRequest;
import com.joejoe2.chat.data.PageRequestWithSince;
import com.joejoe2.chat.data.SliceList;
import com.joejoe2.chat.data.channel.profile.PublicChannelProfile;
import com.joejoe2.chat.data.message.PublicMessageDto;
import com.joejoe2.chat.models.User;
import com.joejoe2.chat.repository.channel.PublicChannelRepository;
import com.joejoe2.chat.repository.message.PublicMessageRepository;
import com.joejoe2.chat.repository.user.UserRepository;
import com.joejoe2.chat.service.channel.PublicChannelService;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(TestContext.class)
class PublicMessageServiceTest {
  @Autowired PublicChannelService channelService;
  @Autowired PublicMessageService messageService;
  @Autowired PublicChannelRepository channelRepository;
  @Autowired PublicMessageRepository messageRepository;
  @Autowired UserRepository userRepository;

  User userA, userB, userC, userD;

  @BeforeEach
  void setUp() {
    userA =
        User.builder()
            .id(UUID.fromString("2354705e-cabf-40dd-b9c5-47a6f1bd5a2d"))
            .userName("A")
            .build();
    userB =
        User.builder()
            .id(UUID.fromString("2354705e-cabf-40dd-b9c5-47a6f1bd5a3d"))
            .userName("B")
            .build();
    userC =
        User.builder()
            .id(UUID.fromString("2354705e-cabf-40dd-b9c5-47a6f1bd5a4d"))
            .userName("C")
            .build();
    userD =
        User.builder()
            .id(UUID.fromString("2354705e-cabf-40dd-b9c5-47a6f1bd5a5d"))
            .userName("D")
            .build();
    userRepository.saveAll(Arrays.asList(userA, userB, userC, userD));
  }

  @AfterEach
  void tearDown() {
    messageRepository.deleteAll();
    channelRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void createMessage() throws Exception {
    // prepare channel
    PublicChannelProfile channel = channelService.createChannel("test");
    // test IllegalArgument
    assertThrows(
        IllegalArgumentException.class,
        () -> messageService.createMessage(userA.getId().toString(), "invalid_id", null));
    // test success
    PublicMessageDto message =
        messageService.createMessage(userA.getId().toString(), channel.getId(), "test");
    assertTrue(messageRepository.existsById(message.getId()));
  }

  @Test
  void getAllMessages() throws Exception {
    // prepare channel
    PublicChannelProfile channel = channelService.createChannel("test");
    // prepare messages
    List<String> messages = new LinkedList<>();
    for (int i = 0; i < 10; i++) {
      messageService.createMessage(userA.getId().toString(), channel.getId(), "a" + i);
      messageService.createMessage(userB.getId().toString(), channel.getId(), "b" + i);
      messageService.createMessage(userC.getId().toString(), channel.getId(), "c" + i);
      messageService.createMessage(userD.getId().toString(), channel.getId(), "d" + i);
      messages.addAll(Arrays.asList("a" + i, "b" + i, "c" + i, "d" + i));
    }
    // test IllegalArgument
    assertThrows(
        IllegalArgumentException.class,
        () ->
            messageService.getAllMessages(
                "invalid_id", PageRequest.builder().page(-1).size(0).build()));
    // test success
    SliceList<PublicMessageDto> sliceList =
        messageService.getAllMessages(
            channel.getId(), PageRequest.builder().page(1).size(10).build());
    assertTrue(sliceList.isHasNext());
    assertEquals(1, sliceList.getCurrentPage());
    assertEquals(10, sliceList.getPageSize());
    assertEquals(
        messages.subList(20, 30),
        sliceList.getList().stream().map(PublicMessageDto::getContent).toList());
  }

  @Test
  void getMessagesSince() throws Exception {
    // prepare channel
    PublicChannelProfile channel = channelService.createChannel("test");
    // prepare messages
    for (int i = 0; i < 10; i++) {
      messageService.createMessage(userA.getId().toString(), channel.getId(), "a" + i);
      messageService.createMessage(userB.getId().toString(), channel.getId(), "b" + i);
      messageService.createMessage(userC.getId().toString(), channel.getId(), "c" + i);
      messageService.createMessage(userD.getId().toString(), channel.getId(), "d" + i);
    }
    Instant since = Instant.now();
    List<String> messages = new LinkedList<>();
    for (int i = 10; i < 20; i++) {
      messageService.createMessage(userA.getId().toString(), channel.getId(), "a" + i);
      messageService.createMessage(userB.getId().toString(), channel.getId(), "b" + i);
      messageService.createMessage(userC.getId().toString(), channel.getId(), "c" + i);
      messageService.createMessage(userD.getId().toString(), channel.getId(), "d" + i);
      messages.addAll(Arrays.asList("a" + i, "b" + i, "c" + i, "d" + i));
    }
    // test IllegalArgument
    assertThrows(
        IllegalArgumentException.class,
        () ->
            messageService.getAllMessages(
                "invalid_id", null, PageRequest.builder().page(-1).size(0).build()));
    // test success
    SliceList<PublicMessageDto> sliceList =
        messageService.getAllMessages(
            channel.getId(), since, PageRequest.builder().page(2).size(10).build());
    assertTrue(sliceList.isHasNext());
    assertEquals(2, sliceList.getCurrentPage());
    assertEquals(10, sliceList.getPageSize());
    assertEquals(
        messages.subList(10, 20),
        sliceList.getList().stream().map(PublicMessageDto::getContent).toList());
  }
}
