package com.joejoe2.chat.service.message;

import static org.junit.jupiter.api.Assertions.*;

import com.joejoe2.chat.TestContext;
import com.joejoe2.chat.data.PageRequest;
import com.joejoe2.chat.data.PageRequestWithSince;
import com.joejoe2.chat.data.SliceList;
import com.joejoe2.chat.data.channel.profile.GroupChannelProfile;
import com.joejoe2.chat.data.message.GroupMessageDto;
import com.joejoe2.chat.exception.InvalidOperation;
import com.joejoe2.chat.models.User;
import com.joejoe2.chat.repository.channel.GroupChannelRepository;
import com.joejoe2.chat.repository.message.GroupMessageRepository;
import com.joejoe2.chat.repository.user.UserRepository;
import com.joejoe2.chat.service.channel.GroupChannelService;
import java.time.Instant;
import java.util.*;

import com.joejoe2.chat.service.nats.NatsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(TestContext.class)
public class GroupMessageServiceTest {
  @Autowired GroupChannelService channelService;

  @Autowired GroupMessageService messageService;

  @Autowired UserRepository userRepository;

  @Autowired GroupChannelRepository channelRepository;

  @Autowired GroupMessageRepository messageRepository;
  @SpyBean
  NatsService natsService;
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
    channelRepository.deleteAll();
    messageRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void createMessage() throws Exception {
    // prepare
    GroupChannelProfile channel = channelService.createChannel(userA.getId().toString(), "test");
    // test success
    GroupMessageDto message =
        messageService.createMessage(userA.getId().toString(), channel.getId(), "test");
    assertTrue(messageRepository.existsById(message.getId()));
  }

  @Test
  void createMessageWithError() throws Exception {
    // prepare
    GroupChannelProfile channel = channelService.createChannel(userA.getId().toString(), "test");
    // test IllegalArgument
    assertThrows(
        IllegalArgumentException.class,
        () -> messageService.createMessage("invalid_uid", "invalid_id", null));
    // test with non member
    assertThrows(
        InvalidOperation.class,
        () -> messageService.createMessage(userB.getId().toString(), channel.getId(), "test"));
  }

  @Test
  void getAllMessages() throws Exception {
    // prepare
    GroupChannelProfile channel = channelService.createChannel(userA.getId().toString(), "test");
    Instant since = Instant.now();
    LinkedList<String> messages = new LinkedList<>();
    for (int i = 0; i < 10; i++) {
      messageService.createMessage(userA.getId().toString(), channel.getId(), "msg" + i);
      messages.add("msg" + i);
    }
    Thread.sleep(100);
    // test success
    SliceList<GroupMessageDto> slice =
        messageService.getAllMessages(
            userA.getId().toString(),
            channel.getId(),
            since,
            PageRequest.builder().page(1).size(5).build());
    assertEquals(1, slice.getCurrentPage());
    assertEquals(5, slice.getPageSize());
    assertFalse(slice.isHasNext());
    for (GroupMessageDto message : slice.getList()) {
      assertEquals(messages.removeFirst(), message.getContent());
    }
    slice =
        messageService.getAllMessages(
            userA.getId().toString(),
            channel.getId(),
            since,
            PageRequest.builder().page(0).size(5).build());
    assertEquals(0, slice.getCurrentPage());
    assertEquals(5, slice.getPageSize());
    assertTrue(slice.isHasNext());
    for (GroupMessageDto message : slice.getList()) {
      assertEquals(messages.removeFirst(), message.getContent());
    }
  }

  @Test
  void getAllMessagesWithError() throws Exception {
    // prepare
    GroupChannelProfile channel = channelService.createChannel(userA.getId().toString(), "test");
    // test IllegalArgument
    assertThrows(
        IllegalArgumentException.class,
        () ->
            messageService.getAllMessages(
                "invalid_uid", "invalid_id", null, PageRequest.builder().page(-1).size(0).build()));
    // test with non member
    assertThrows(
        InvalidOperation.class,
        () ->
            messageService.getAllMessages(
                userB.getId().toString(),
                channel.getId(),
                Instant.now(),
                PageRequest.builder().page(0).size(1).build()));
  }

  @Test
  void deliverMessageToInvitee() throws Exception{
    // prepare
    GroupChannelProfile channel = channelService.createChannel(userA.getId().toString(), "test");

    // member and invitee
    GroupMessageDto message = channelService.inviteToChannel(userA.getId().toString(), userB.getId().toString(), channel.getId());
    messageService.deliverMessage(message);
    Thread.sleep(1000);
    Mockito.verify(natsService, Mockito.times(2)).publish(Mockito.any(), Mockito.eq(message));
    message = channelService.inviteToChannel(userA.getId().toString(), userC.getId().toString(), channel.getId());
    messageService.deliverMessage(message);
    Thread.sleep(1000);
    Mockito.verify(natsService, Mockito.times(2)).publish(Mockito.any(), Mockito.eq(message));

    // accept invitation
    message = channelService.acceptInvitationOfChannel(userB.getId().toString(), channel.getId());
    messageService.deliverMessage(message);
    Thread.sleep(1000);
    Mockito.verify(natsService, Mockito.times(2)).publish(Mockito.any(), Mockito.eq(message));
    message = channelService.acceptInvitationOfChannel(userC.getId().toString(), channel.getId());
    messageService.deliverMessage(message);
    Thread.sleep(1000);
    Mockito.verify(natsService, Mockito.times(3)).publish(Mockito.any(), Mockito.eq(message));

    // normal messages
    message = messageService.createMessage(userA.getId().toString(), channel.getId(), "msg");
    messageService.deliverMessage(message);
    Thread.sleep(1000);
    Mockito.verify(natsService, Mockito.times(3)).publish(Mockito.any(), Mockito.eq(message));

    // member and leaver
    message = channelService.leaveChannel(userB.getId().toString(), channel.getId());
    messageService.deliverMessage(message);
    Thread.sleep(1000);
    Mockito.verify(natsService, Mockito.times(3)).publish(Mockito.any(), Mockito.eq(message));

    // member and leaver(by kick off)
    message = channelService.removeFromChannel(userA.getId().toString(), userC.getId().toString(), channel.getId());
    messageService.deliverMessage(message);
    Thread.sleep(1000);
    Mockito.verify(natsService, Mockito.times(2)).publish(Mockito.any(), Mockito.eq(message));
  }

  @Test
  void getInvitations() throws Exception{
    // prepare
    Instant since = Instant.now();
    List<GroupMessageDto> messages = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      GroupChannelProfile channel = channelService.createChannel(userA.getId().toString(), "test"+i);
      GroupMessageDto message = channelService.inviteToChannel(
              userA.getId().toString(), userB.getId().toString(), channel.getId());
      messages.add(message);
    }
    // test success
    SliceList<GroupMessageDto> sliceList = messageService.getInvitations(userB.getId().toString(), since, PageRequest.builder()
            .page(0).size(messages.size()).build());
    assertEquals(messages, sliceList.getList());
  }
}
