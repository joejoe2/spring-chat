package com.joejoe2.chat.service.channel;

import static org.junit.jupiter.api.Assertions.*;

import com.joejoe2.chat.TestContext;
import com.joejoe2.chat.data.PageRequest;
import com.joejoe2.chat.data.SliceList;
import com.joejoe2.chat.data.UserPublicProfile;
import com.joejoe2.chat.data.channel.profile.PrivateChannelProfile;
import com.joejoe2.chat.exception.AlreadyExist;
import com.joejoe2.chat.exception.InvalidOperation;
import com.joejoe2.chat.models.User;
import com.joejoe2.chat.repository.channel.PrivateChannelRepository;
import com.joejoe2.chat.repository.message.PrivateMessageRepository;
import com.joejoe2.chat.repository.user.UserRepository;
import java.util.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(TestContext.class)
class PrivateChannelServiceTest {
  @Autowired PrivateChannelService channelService;
  @Autowired PrivateChannelRepository channelRepository;
  @Autowired PrivateMessageRepository messageRepository;
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
    channelRepository.deleteAll();
    messageRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void createChannelBetween() {
    // test IllegalArgument
    assertThrows(
        IllegalArgumentException.class,
        () -> channelService.createChannelBetween("invalid_uid", "invalid_uid"));
    // test InvalidOperation, chat with self
    assertThrows(
        InvalidOperation.class,
        () ->
            channelService.createChannelBetween(
                userA.getId().toString(), userA.getId().toString()));
    // test success
    assertDoesNotThrow(
        () -> {
          PrivateChannelProfile channel;
          for (User user : Arrays.asList(userB, userC, userD)) {
            channel =
                channelService.createChannelBetween(
                    userA.getId().toString(), user.getId().toString());
            assertEquals(
                new HashSet<>(channel.getMembers()),
                new HashSet<>(
                    Arrays.asList(new UserPublicProfile(userA), new UserPublicProfile(user))));
          }
          for (User user : Arrays.asList(userC, userD)) {
            channel =
                channelService.createChannelBetween(
                    userB.getId().toString(), user.getId().toString());
            assertEquals(
                new HashSet<>(channel.getMembers()),
                new HashSet<>(
                    Arrays.asList(new UserPublicProfile(userB), new UserPublicProfile(user))));
          }
          channel =
              channelService.createChannelBetween(
                  userC.getId().toString(), userD.getId().toString());
          assertEquals(
              new HashSet<>(channel.getMembers()),
              new HashSet<>(
                  Arrays.asList(new UserPublicProfile(userC), new UserPublicProfile(userD))));
        });
    // test AlreadyExist
    assertThrows(
        AlreadyExist.class,
        () ->
            channelService.createChannelBetween(
                userA.getId().toString(), userB.getId().toString()));
  }

  @Test
  void getAllChannels() throws Exception {
    // prepare channels
    PrivateChannelProfile channelOfUserC =
        channelService.createChannelBetween(userC.getId().toString(), userD.getId().toString());
    Stack<PrivateChannelProfile> channelsOfUserA = new Stack<>();
    for (User user : Arrays.asList(userB, userC, userD)) {
      channelsOfUserA.add(
          channelService.createChannelBetween(userA.getId().toString(), user.getId().toString()));
    }
    List<PrivateChannelProfile> channelsOfUserB = new ArrayList<>();
    for (User user : Arrays.asList(userC, userD)) {
      channelsOfUserB.add(
          channelService.createChannelBetween(userB.getId().toString(), user.getId().toString()));
    }
    // test IllegalArgument
    assertThrows(
        IllegalArgumentException.class,
        () ->
            channelService.getAllChannels(
                "invalid_uid", PageRequest.builder().page(-1).size(0).build()));
    // test success
    SliceList<PrivateChannelProfile> channels =
        channelService.getAllChannels(
            userA.getId().toString(), PageRequest.builder().page(0).size(2).build());
    assertEquals(channels.getPageSize(), 2);
    assertEquals(channels.getCurrentPage(), 0);
    assertTrue(channels.isHasNext());
    // order by updateAt desc
    for (int i = 0; i < channels.getList().size(); i++) {
      assertEquals(channelsOfUserA.pop(), channels.getList().get(i));
    }
    // different page
    channels =
        channelService.getAllChannels(
            userB.getId().toString(), PageRequest.builder().page(1).size(1).build());
    assertEquals(channels.getPageSize(), 1);
    assertEquals(channels.getCurrentPage(), 1);
    assertTrue(channels.isHasNext());
    for (int i = 0; i < channels.getList().size(); i++) {
      assertEquals(channelsOfUserB.get(i), channels.getList().get(i));
    }
    // last page
    channels =
        channelService.getAllChannels(
            userC.getId().toString(), PageRequest.builder().page(2).size(1).build());
    assertEquals(channels.getPageSize(), 1);
    assertEquals(channels.getCurrentPage(), 2);
    assertFalse(channels.isHasNext());
    assertEquals(channelOfUserC, channels.getList().get(0));
  }

  @Test
  void getChannelProfile() throws Exception {
    // prepare channels
    PrivateChannelProfile channel =
        channelService.createChannelBetween(userA.getId().toString(), userB.getId().toString());
    // test IllegalArgument
    assertThrows(IllegalArgumentException.class, () -> channelService.getChannelProfile("", ""));
    // test InvalidOperation
    assertThrows(
        InvalidOperation.class,
        () -> channelService.getChannelProfile(userC.getId().toString(), channel.getId()));
    // test success
    assertEquals(
        channel, channelService.getChannelProfile(userA.getId().toString(), channel.getId()));
    assertEquals(
        channel, channelService.getChannelProfile(userB.getId().toString(), channel.getId()));
  }

  @Test
  @Transactional
  void setBlockage() throws Exception {
    // prepare channels
    PrivateChannelProfile channel =
        channelService.createChannelBetween(userA.getId().toString(), userB.getId().toString());
    // test IllegalArgument
    assertThrows(
        IllegalArgumentException.class,
        () -> channelService.block("invalid_id", "invalid_id", true));
    // test InvalidOperation
    assertThrows(
        InvalidOperation.class,
        () -> channelService.block(userC.getId().toString(), channel.getId(), true));
    // test success
    channelService.block(userA.getId().toString(), channel.getId(), true);
    assertTrue(channelRepository.getById(UUID.fromString(channel.getId())).isBlocked(userB));
    channelService.block(userB.getId().toString(), channel.getId(), true);
    assertTrue(channelRepository.getById(UUID.fromString(channel.getId())).isBlocked(userA));
    channelService.block(userA.getId().toString(), channel.getId(), false);
    assertFalse(channelRepository.getById(UUID.fromString(channel.getId())).isBlocked(userB));
    channelService.block(userB.getId().toString(), channel.getId(), false);
    assertFalse(channelRepository.getById(UUID.fromString(channel.getId())).isBlocked(userA));
  }

  @Test
  void getChannelsBlockedByUser() throws Exception {
    // prepare channels
    Stack<PrivateChannelProfile> channelsOfUserA = new Stack<>();
    for (User user : Arrays.asList(userB, userC, userD)) {
      PrivateChannelProfile channel =
          channelService.createChannelBetween(userA.getId().toString(), user.getId().toString());
      channelsOfUserA.add(channel);
      channelService.block(userA.getId().toString(), channel.getId(), true);
    }
    List<PrivateChannelProfile> channelsOfUserB = new ArrayList<>();
    for (User user : Arrays.asList(userC, userD)) {
      PrivateChannelProfile channel =
          channelService.createChannelBetween(userB.getId().toString(), user.getId().toString());
      channelsOfUserB.add(channel);
      channelService.block(userB.getId().toString(), channel.getId(), true);
    }
    // test IllegalArgument
    assertThrows(
        IllegalArgumentException.class,
        () ->
            channelService.getChannelsBlockedByUser(
                "invalid_uid", PageRequest.builder().page(-1).size(0).build()));
    // test success
    SliceList<PrivateChannelProfile> channels =
        channelService.getChannelsBlockedByUser(
            userA.getId().toString(), PageRequest.builder().page(0).size(2).build());
    assertEquals(channels.getPageSize(), 2);
    assertEquals(channels.getCurrentPage(), 0);
    assertTrue(channels.isHasNext());
    // order by updateAt desc
    for (int i = 0; i < channels.getList().size(); i++) {
      assertEquals(channelsOfUserA.pop(), channels.getList().get(i));
    }
    // last page
    channels =
        channelService.getChannelsBlockedByUser(
            userB.getId().toString(), PageRequest.builder().page(1).size(1).build());
    assertEquals(channels.getPageSize(), 1);
    assertEquals(channels.getCurrentPage(), 1);
    assertFalse(channels.isHasNext());
    for (int i = 0; i < channels.getList().size(); i++) {
      assertEquals(channelsOfUserB.get(i), channels.getList().get(i));
    }
    // 0 page
    channels =
        channelService.getChannelsBlockedByUser(
            userC.getId().toString(), PageRequest.builder().page(0).size(1).build());
    assertEquals(channels.getPageSize(), 1);
    assertEquals(channels.getCurrentPage(), 0);
    assertFalse(channels.isHasNext());
    assertEquals(0, channels.getList().size());
  }
}
