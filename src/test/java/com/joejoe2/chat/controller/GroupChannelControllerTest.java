package com.joejoe2.chat.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joejoe2.chat.TestContext;
import com.joejoe2.chat.data.PageRequest;
import com.joejoe2.chat.data.SliceList;
import com.joejoe2.chat.data.UserDetail;
import com.joejoe2.chat.data.channel.request.ChannelPageRequestWithSince;
import com.joejoe2.chat.data.channel.request.ChannelRequest;
import com.joejoe2.chat.data.channel.request.ChannelUserRequest;
import com.joejoe2.chat.data.message.GroupMessageDto;
import com.joejoe2.chat.data.message.MessageDto;
import com.joejoe2.chat.data.message.SliceOfMessage;
import com.joejoe2.chat.data.message.request.PublishMessageRequest;
import com.joejoe2.chat.models.GroupChannel;
import com.joejoe2.chat.models.User;
import com.joejoe2.chat.repository.channel.GroupChannelRepository;
import com.joejoe2.chat.repository.message.GroupMessageRepository;
import com.joejoe2.chat.repository.user.UserRepository;
import com.joejoe2.chat.service.message.GroupMessageService;
import com.joejoe2.chat.service.nats.NatsService;
import com.joejoe2.chat.utils.AuthUtil;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.LinkedMultiValueMap;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(TestContext.class)
public class GroupChannelControllerTest {
  MockedStatic<AuthUtil> mockAuthUtil;
  User user1, user2, user3, otherUser;
  GroupChannel channel;
  @Autowired UserRepository userRepository;
  @Autowired GroupChannelRepository channelRepository;
  @Autowired GroupMessageRepository messageRepository;
  @SpyBean GroupMessageService messageService;
  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;
  @SpyBean NatsService natsService;

  @BeforeEach
  void setUp() {
    user1 = User.builder().id(UUID.randomUUID()).userName("test1").build();
    userRepository.save(user1);
    user2 = User.builder().id(UUID.randomUUID()).userName("test2").build();
    userRepository.save(user2);
    user3 = User.builder().id(UUID.randomUUID()).userName("test3").build();
    userRepository.save(user3);
    otherUser = User.builder().id(UUID.randomUUID()).userName("test4").build();
    userRepository.save(otherUser);
    channel = new GroupChannel(Set.of(user1, user2, user3));
    channelRepository.save(channel);
    // mock login
    mockAuthUtil = Mockito.mockStatic(AuthUtil.class);
    mockAuthUtil.when(AuthUtil::isAuthenticated).thenReturn(true);
    mockAuthUtil.when(AuthUtil::currentUserDetail).thenReturn(new UserDetail(user1));
  }

  @AfterEach
  void tearDown() {
    channelRepository.deleteAll();
    userRepository.deleteAll();
    mockAuthUtil.close();
  }

  @Test
  void publishMessage() throws Exception {
    // test success
    PublishMessageRequest request =
        PublishMessageRequest.builder()
            .channelId(channel.getId().toString())
            .message("msg")
            .build();
    Instant since = Instant.now();
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/channel/group/publishMessage")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
    GroupMessageDto message =
        objectMapper.readValue(result.getResponse().getContentAsString(), GroupMessageDto.class);
    GroupMessageDto message1 =
        messageService
            .getAllMessages(
                user1.getId().toString(),
                channel.getId().toString(),
                since,
                PageRequest.builder().page(0).size(1).build())
            .getList()
            .get(0);
    assertEquals(message, message1);
    Mockito.verify(messageService, Mockito.times(1)).deliverMessage(Mockito.any());
  }

  @Test
  void publishMessageWithError() throws Exception {
    PublishMessageRequest request =
        PublishMessageRequest.builder().channelId("invalid id").message(" ").build();
    // test 400
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/channel/group/publishMessage")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.errors.channelId").exists())
        .andExpect(jsonPath("$.errors.message").exists())
        .andExpect(status().isBadRequest());
    // test 404
    String id = UUID.randomUUID().toString();
    while (Objects.equals(channel.getId().toString(), id)) id = UUID.randomUUID().toString();
    request = PublishMessageRequest.builder().channelId(id).message("msg").build();

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/channel/group/publishMessage")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.message").exists())
        .andExpect(status().isNotFound());
    // test 403
    request =
        PublishMessageRequest.builder()
            .channelId(channel.getId().toString())
            .message("msg")
            .build();
    // override mock login
    mockAuthUtil.when(AuthUtil::currentUserDetail).thenReturn(new UserDetail(otherUser));
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/channel/group/publishMessage")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.message").exists())
        .andExpect(status().isForbidden());
  }

  @Test
  void getMessages() throws Exception {
    // prepare
    Instant since = Instant.now();
    List<GroupMessageDto> messages = new ArrayList<>();
    for (int i = 0; i < 10; i++)
      messages.add(
          messageService.createMessage(
              user1.getId().toString(), channel.getId().toString(), "msg"));
    ChannelPageRequestWithSince request =
        ChannelPageRequestWithSince.builder()
            .pageRequest(PageRequest.builder().page(0).size(messages.size()).build())
            .channelId(channel.getId().toString())
            .since(since)
            .build();
    LinkedMultiValueMap<String, String> query = new LinkedMultiValueMap<>();
    query.add("channelId", request.getChannelId());
    query.add("pageRequest.page", request.getPageRequest().getPage().toString());
    query.add("pageRequest.size", request.getPageRequest().getSize().toString());
    query.add("since", request.getSince().toString());

    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/api/channel/group/getMessagesSince")
                    .params(query)
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
    SliceOfMessage<MessageDto> slice =
        objectMapper.readValue(result.getResponse().getContentAsString(), SliceOfMessage.class);
    assertEquals(request.getPageRequest().getSize(), slice.getPageSize());
    assertEquals(request.getPageRequest().getPage(), slice.getCurrentPage());
    assertEquals(request.getPageRequest().getSize(), slice.getMessages().size());
    for (int i = 0; i < messages.size(); i++) {
      assertEquals(messages.get(i), slice.getMessages().get(i));
    }
  }

  @Test
  void getMessagesWithError() throws Exception {
    // test 400
    LinkedMultiValueMap<String, String> query = new LinkedMultiValueMap<>();
    query.add("channelId", "");
    query.add("pageRequest.page", "-1");
    query.add("pageRequest.size", "0");
    query.add("since", "");
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/channel/group/getMessagesSince")
                .params(query)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.errors.channelId").exists())
        .andExpect(jsonPath("$.errors.['pageRequest.page']").exists())
        .andExpect(jsonPath("$.errors.['pageRequest.size']").exists())
        .andExpect(jsonPath("$.errors.since").exists())
        .andExpect(status().isBadRequest());
  }

  @Test
  void inviteThenAccept() throws Exception {
    Instant since = Instant.now();
    // invite
    ChannelUserRequest request =
        ChannelUserRequest.builder()
            .channelId(channel.getId().toString())
            .targetUserId(otherUser.getId().toString())
            .build();
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/channel/group/invite")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
    GroupMessageDto message =
        objectMapper.readValue(result.getResponse().getContentAsString(), GroupMessageDto.class);
    Thread.sleep(1000);
    Mockito.verify(messageService, Mockito.times(1)).deliverMessage(message);
    // get invitations
    LinkedMultiValueMap<String, String> query = new LinkedMultiValueMap<>();
    query.add("channelId", channel.getId().toString());
    query.add("pageRequest.page", "0");
    query.add("pageRequest.size", "1");
    query.add("since", since.toString());
    // override mock login
    mockAuthUtil.when(AuthUtil::currentUserDetail).thenReturn(new UserDetail(otherUser));
    result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/api/channel/group/invitation")
                    .params(query)
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
    SliceList<String> sliceList =
        objectMapper.readValue(result.getResponse().getContentAsString(), SliceList.class);
    assertEquals(channel.getId().toString(), sliceList.getList().get(0));
    // accept
    result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/channel/group/accept")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
    message =
        objectMapper.readValue(result.getResponse().getContentAsString(), GroupMessageDto.class);
    Thread.sleep(1000);
    Mockito.verify(messageService, Mockito.times(1)).deliverMessage(message);
    assertTrue(channelRepository.findByIsUserInMembers(otherUser, since).contains(channel));
  }

  @Test
  void inviteWithError() throws Exception {
    // test 403
    // invite a member
    ChannelUserRequest request =
        ChannelUserRequest.builder()
            .channelId(channel.getId().toString())
            .targetUserId(user2.getId().toString())
            .build();
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/channel/group/invite")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.message").exists())
        .andExpect(status().isForbidden());
    // invite a invited user
    request =
        ChannelUserRequest.builder()
            .channelId(channel.getId().toString())
            .targetUserId(otherUser.getId().toString())
            .build();
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/channel/group/invite")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
    request =
        ChannelUserRequest.builder()
            .channelId(channel.getId().toString())
            .targetUserId(otherUser.getId().toString())
            .build();
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/channel/group/invite")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.message").exists())
        .andExpect(status().isForbidden());
  }

  @Test
  void acceptWithError() throws Exception {
    // test 403
    // no invitation
    ChannelRequest request = ChannelRequest.builder().channelId(channel.getId().toString()).build();
    // override mock login
    mockAuthUtil.when(AuthUtil::currentUserDetail).thenReturn(new UserDetail(otherUser));
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/channel/group/accept")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.message").exists())
        .andExpect(status().isForbidden());
  }

  @Test
  void kickOff() throws Exception {
    Instant since = Instant.now();
    ChannelUserRequest request =
        ChannelUserRequest.builder()
            .channelId(channel.getId().toString())
            .targetUserId(user2.getId().toString())
            .build();
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/channel/group/kickOff")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
    GroupMessageDto message =
        objectMapper.readValue(result.getResponse().getContentAsString(), GroupMessageDto.class);
    Thread.sleep(1000);
    Mockito.verify(messageService, Mockito.times(1)).deliverMessage(message);
    assertFalse(channelRepository.findByIsUserInMembers(user2, since).contains(channel));
  }

  @Test
  void kickOffWithError() throws Exception {
    // test 403
    // kick off self
    ChannelUserRequest request =
        ChannelUserRequest.builder()
            .channelId(channel.getId().toString())
            .targetUserId(user1.getId().toString())
            .build();
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/channel/group/kickOff")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.message").exists())
        .andExpect(status().isForbidden());
    // kick off non member
    request =
        ChannelUserRequest.builder()
            .channelId(channel.getId().toString())
            .targetUserId(otherUser.getId().toString())
            .build();
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/channel/group/kickOff")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.message").exists())
        .andExpect(status().isForbidden());
  }

  @Test
  void leave() throws Exception {
    Instant since = Instant.now();
    ChannelRequest request = ChannelRequest.builder().channelId(channel.getId().toString()).build();
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/channel/group/leave")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
    GroupMessageDto message =
        objectMapper.readValue(result.getResponse().getContentAsString(), GroupMessageDto.class);
    Thread.sleep(1000);
    Mockito.verify(messageService, Mockito.times(1)).deliverMessage(message);
    assertFalse(channelRepository.findByIsUserInMembers(user1, since).contains(channel));
  }

  @Test
  void leaveWithError() throws Exception {
    // test 403
    // not a member
    ChannelRequest request = ChannelRequest.builder().channelId(channel.getId().toString()).build();
    // override mock login
    mockAuthUtil.when(AuthUtil::currentUserDetail).thenReturn(new UserDetail(otherUser));
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/channel/group/leave")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.message").exists())
        .andExpect(status().isForbidden());
  }

  @Test
  void list() {}
}
