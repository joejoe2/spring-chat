package com.joejoe2.chat.service.channel;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joejoe2.chat.TestContext;
import com.joejoe2.chat.data.PageRequest;
import com.joejoe2.chat.data.SliceList;
import com.joejoe2.chat.data.UserPublicProfile;
import com.joejoe2.chat.data.channel.profile.GroupChannelProfile;
import com.joejoe2.chat.data.message.GroupMessageDto;
import com.joejoe2.chat.exception.InvalidOperation;
import com.joejoe2.chat.models.MessageType;
import com.joejoe2.chat.models.User;
import com.joejoe2.chat.repository.channel.GroupChannelRepository;
import com.joejoe2.chat.repository.message.GroupMessageRepository;
import com.joejoe2.chat.repository.user.UserRepository;
import java.time.Instant;
import java.util.Arrays;
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
public class GroupChannelServiceTest {
  @Autowired GroupChannelService channelService;
  @Autowired GroupChannelRepository channelRepository;
  @Autowired GroupMessageRepository messageRepository;
  @Autowired UserRepository userRepository;
  @Autowired ObjectMapper objectMapper;
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
    userRepository.saveAllAndFlush(Arrays.asList(userA, userB, userC, userD));
  }

  @AfterEach
  void tearDown() {
    channelRepository.deleteAll();
    messageRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void createChannel() throws Exception {
    // test IllegalArgument
    assertThrows(
        IllegalArgumentException.class, () -> channelService.createChannel("invalid_uid", ""));
    // test success
    GroupChannelProfile channelProfile =
        channelService.createChannel(userA.getId().toString(), "test");
    assertEquals(List.of(new UserPublicProfile(userA)), channelProfile.getMembers());
    assertEquals("test", channelProfile.getName());
  }

  @Test
  void inviteToChannel() throws Exception {
    // prepare
    GroupChannelProfile channel = channelService.createChannel(userA.getId().toString(), "test");
    // test success
    GroupMessageDto message =
        channelService.inviteToChannel(
            userA.getId().toString(), userB.getId().toString(), channel.getId());
    assertEquals(MessageType.INVITATION, message.getMessageType());
    assertEquals(new UserPublicProfile(userA), message.getFrom());
    assertEquals(
        new UserPublicProfile(userB),
        objectMapper.readValue(message.getContent(), UserPublicProfile.class));
  }

  @Test
  void inviteToChannelWithError() throws Exception {
    // prepare
    GroupChannelProfile channel = channelService.createChannel(userA.getId().toString(), "test");
    // test IllegalArgument
    assertThrows(
        IllegalArgumentException.class,
        () -> channelService.inviteToChannel("invalid_uid", "invalid_uid", "invalid_id"));
    // test if inviter is not in channel
    assertThrows(
        InvalidOperation.class,
        () ->
            channelService.inviteToChannel(
                userC.getId().toString(), userB.getId().toString(), channel.getId()));
    // test if invitee is already in channel
    assertThrows(
        InvalidOperation.class,
        () ->
            channelService.inviteToChannel(
                userA.getId().toString(), userA.getId().toString(), channel.getId()));
  }

  @Test
  void acceptInvitationOfChannel() throws Exception {
    // prepare
    GroupChannelProfile channel = channelService.createChannel(userA.getId().toString(), "test");
    // test success
    channelService.inviteToChannel(
        userA.getId().toString(), userB.getId().toString(), channel.getId());
    GroupMessageDto message =
        channelService.acceptInvitationOfChannel(userB.getId().toString(), channel.getId());
    channel = channelService.getChannelProfile(userA.getId().toString(), channel.getId());
    assertEquals(MessageType.JOIN, message.getMessageType());
    assertEquals(new UserPublicProfile(userB), message.getFrom());
    assertEquals(
        new UserPublicProfile(userB),
        objectMapper.readValue(message.getContent(), UserPublicProfile.class));
    assertTrue(channel.getMembers().contains(new UserPublicProfile(userA)));
    assertTrue(channel.getMembers().contains(new UserPublicProfile(userB)));
  }

  @Test
  void acceptInvitationOfChannelWithError() throws Exception {
    // test IllegalArgument
    assertThrows(
        IllegalArgumentException.class,
        () -> channelService.acceptInvitationOfChannel("invalid_uid", "invalid_id"));
    // prepare
    GroupChannelProfile channel = channelService.createChannel(userA.getId().toString(), "test");
    channelService.inviteToChannel(
        userA.getId().toString(), userB.getId().toString(), channel.getId());
    // test for non invitee
    assertThrows(
        InvalidOperation.class,
        () -> channelService.acceptInvitationOfChannel(userC.getId().toString(), channel.getId()));
  }

  @Test
  void getAllChannels() throws Exception {
    // prepare
    Instant since = Instant.now();
    Thread.sleep(100);
    GroupChannelProfile channel1 = channelService.createChannel(userA.getId().toString(), "test");
    GroupChannelProfile channel2 = channelService.createChannel(userB.getId().toString(), "test");
    channelService.inviteToChannel(
        userB.getId().toString(), userA.getId().toString(), channel2.getId());
    channelService.acceptInvitationOfChannel(userA.getId().toString(), channel2.getId());
    channelService.inviteToChannel(
        userA.getId().toString(), userB.getId().toString(), channel1.getId());
    channelService.acceptInvitationOfChannel(userB.getId().toString(), channel1.getId());
    // test success
    SliceList<GroupChannelProfile> slice =
        channelService.getAllChannels(
            userA.getId().toString(), since, PageRequest.builder().page(0).size(2).build());
    assertEquals(0, slice.getCurrentPage());
    assertEquals(2, slice.getPageSize());
    assertFalse(slice.isHasNext());
    assertEquals(2, slice.getList().size());
    // order by updateAt desc
    assertEquals(
        channelService.getChannelProfile(userA.getId().toString(), channel1.getId()),
        slice.getList().get(0));
    assertEquals(
        channelService.getChannelProfile(userB.getId().toString(), channel2.getId()),
        slice.getList().get(1));
  }

  @Test
  void getAllChannelsWithError() throws Exception {
    // test IllegalArgument
    assertThrows(
        IllegalArgumentException.class,
        () ->
            channelService.getAllChannels(
                "invalid_uid", Instant.now(), PageRequest.builder().page(-1).size(0).build()));
  }

  @Test
  void getChannelProfile() throws Exception {
    // prepare
    GroupChannelProfile channel = channelService.createChannel(userA.getId().toString(), "test");
    for (User user : Arrays.asList(userB, userC, userD)) {
      channelService.inviteToChannel(
          userA.getId().toString(), user.getId().toString(), channel.getId());
      channelService.acceptInvitationOfChannel(user.getId().toString(), channel.getId());
    }
    // test success
    channel = channelService.getChannelProfile(userA.getId().toString(), channel.getId());
    assertEquals(
        channel.getMembers(),
        List.of(userA, userB, userC, userD).stream().map(UserPublicProfile::new).toList());
  }

  @Test
  void getChannelProfileWithError() throws Exception {
    // test IllegalArgument
    assertThrows(
        IllegalArgumentException.class,
        () -> channelService.getChannelProfile("invalid_uid", "invalid_id"));
    // prepare
    GroupChannelProfile channel = channelService.createChannel(userA.getId().toString(), "test");
    channelService.inviteToChannel(
        userA.getId().toString(), userC.getId().toString(), channel.getId());
    // test for non member and pending user
    assertThrows(
        InvalidOperation.class,
        () -> channelService.getChannelProfile(userB.getId().toString(), channel.getId()));
    assertThrows(
        InvalidOperation.class,
        () -> channelService.getChannelProfile(userC.getId().toString(), channel.getId()));
  }

  @Test
  void removeFromChannel() throws Exception {
    // prepare
    GroupChannelProfile channel = channelService.createChannel(userA.getId().toString(), "test");
    for (User user : Arrays.asList(userB, userC, userD)) {
      channelService.inviteToChannel(
          userA.getId().toString(), user.getId().toString(), channel.getId());
      channelService.acceptInvitationOfChannel(user.getId().toString(), channel.getId());
    }
    // test success
    channelService.removeFromChannel(
        userA.getId().toString(), userD.getId().toString(), channel.getId());
    assertFalse(
        channelService
            .getChannelProfile(userA.getId().toString(), channel.getId())
            .getMembers()
            .contains(new UserPublicProfile(userD)));
    channelService.removeFromChannel(
        userA.getId().toString(), userC.getId().toString(), channel.getId());
    assertFalse(
        channelService
            .getChannelProfile(userA.getId().toString(), channel.getId())
            .getMembers()
            .contains(new UserPublicProfile(userC)));
  }

  @Test
  void removeFromChannelWithError() throws Exception {
    // test IllegalArgument
    assertThrows(
        IllegalArgumentException.class,
        () -> channelService.removeFromChannel("invalid_uid", "invalid_uid", "invalid_id"));
    // prepare
    GroupChannelProfile channel = channelService.createChannel(userA.getId().toString(), "test");
    for (User user : Arrays.asList(userB, userC)) {
      channelService.inviteToChannel(
          userA.getId().toString(), user.getId().toString(), channel.getId());
      channelService.acceptInvitationOfChannel(user.getId().toString(), channel.getId());
    }
    // test with self
    assertThrows(
        InvalidOperation.class,
        () ->
            channelService.removeFromChannel(
                userA.getId().toString(), userA.getId().toString(), channel.getId()));
    // test with non member
    assertThrows(
        InvalidOperation.class,
        () ->
            channelService.removeFromChannel(
                userA.getId().toString(), userD.getId().toString(), channel.getId()));
  }

  @Test
  void leaveChannel() throws Exception {
    // prepare
    GroupChannelProfile channel = channelService.createChannel(userA.getId().toString(), "test");
    for (User user : Arrays.asList(userB, userC, userD)) {
      channelService.inviteToChannel(
          userA.getId().toString(), user.getId().toString(), channel.getId());
      channelService.acceptInvitationOfChannel(user.getId().toString(), channel.getId());
    }
    // test success
    channelService.leaveChannel(userD.getId().toString(), channel.getId());
    assertFalse(
        channelService
            .getChannelProfile(userA.getId().toString(), channel.getId())
            .getMembers()
            .contains(new UserPublicProfile(userD)));
    channelService.leaveChannel(userC.getId().toString(), channel.getId());
    assertFalse(
        channelService
            .getChannelProfile(userA.getId().toString(), channel.getId())
            .getMembers()
            .contains(new UserPublicProfile(userC)));
  }

  @Test
  void leaveChannelWithError() throws Exception {
    // test IllegalArgument
    assertThrows(
        IllegalArgumentException.class,
        () -> channelService.leaveChannel("invalid_uid", "invalid_id"));
    // prepare
    GroupChannelProfile channel = channelService.createChannel(userA.getId().toString(), "test");
    for (User user : Arrays.asList(userB, userC)) {
      channelService.inviteToChannel(
          userA.getId().toString(), user.getId().toString(), channel.getId());
      channelService.acceptInvitationOfChannel(user.getId().toString(), channel.getId());
    }
    // test with non member
    assertThrows(
        InvalidOperation.class,
        () -> channelService.leaveChannel(userD.getId().toString(), channel.getId()));
    // test with last member
    channelService.leaveChannel(userC.getId().toString(), channel.getId());
    channelService.leaveChannel(userB.getId().toString(), channel.getId());
    assertThrows(
        InvalidOperation.class,
        () -> channelService.leaveChannel(userA.getId().toString(), channel.getId()));
  }
}
