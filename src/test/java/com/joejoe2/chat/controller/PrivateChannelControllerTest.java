package com.joejoe2.chat.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joejoe2.chat.TestContext;
import com.joejoe2.chat.data.PageRequest;
import com.joejoe2.chat.data.SliceList;
import com.joejoe2.chat.data.UserDetail;
import com.joejoe2.chat.data.channel.SliceOfChannel;
import com.joejoe2.chat.data.channel.profile.PrivateChannelProfile;
import com.joejoe2.chat.data.channel.request.CreatePrivateChannelRequest;
import com.joejoe2.chat.data.channel.request.GetChannelProfileRequest;
import com.joejoe2.chat.data.message.MessageDto;
import com.joejoe2.chat.data.message.PrivateMessageDto;
import com.joejoe2.chat.data.message.SliceOfMessage;
import com.joejoe2.chat.data.message.request.GetPrivateMessageSinceRequest;
import com.joejoe2.chat.data.message.request.PublishPrivateMessageRequest;
import com.joejoe2.chat.exception.AlreadyExist;
import com.joejoe2.chat.exception.InvalidOperation;
import com.joejoe2.chat.exception.UserDoesNotExist;
import com.joejoe2.chat.models.PrivateChannel;
import com.joejoe2.chat.models.User;
import com.joejoe2.chat.repository.channel.PrivateChannelRepository;
import com.joejoe2.chat.repository.message.PrivateMessageRepository;
import com.joejoe2.chat.repository.user.UserRepository;
import com.joejoe2.chat.service.channel.PrivateChannelService;
import com.joejoe2.chat.service.message.PrivateMessageService;
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
class PrivateChannelControllerTest {
  MockedStatic<AuthUtil> mockAuthUtil;
  User user1, user2, otherUser;
  PrivateChannel channel;
  @Autowired UserRepository userRepository;
  @Autowired PrivateChannelRepository channelRepository;
  @Autowired PrivateMessageRepository messageRepository;
  @Autowired MockMvc mockMvc;
  @SpyBean PrivateMessageService messageService;
  @SpyBean PrivateChannelService channelService;
  @Autowired ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    user1 = User.builder().id(UUID.randomUUID()).userName("test1").build();
    userRepository.save(user1);
    user2 = User.builder().id(UUID.randomUUID()).userName("test2").build();
    userRepository.save(user2);
    otherUser = User.builder().id(UUID.randomUUID()).userName("test3").build();
    userRepository.save(otherUser);
    channel = new PrivateChannel(Set.of(user1, user2));
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
    PublishPrivateMessageRequest request =
        PublishPrivateMessageRequest.builder()
            .channelId(channel.getId().toString())
            .message("msg")
            .build();
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/channel/private/publishMessage")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

    PrivateMessageDto message =
        objectMapper.readValue(result.getResponse().getContentAsString(), PrivateMessageDto.class);
    PrivateMessageDto message1 =
        messageService
            .getAllMessages(user1.getId().toString(), PageRequest.builder().page(0).size(1).build())
            .getList()
            .get(0);
    PrivateMessageDto message2 =
        messageService
            .getAllMessages(user2.getId().toString(), PageRequest.builder().page(0).size(1).build())
            .getList()
            .get(0);
    assertEquals(message1, message2);
    assertEquals(message1.getId(), message.getId());
    Mockito.verify(messageService, Mockito.times(1)).deliverMessage(Mockito.any());
  }

  @Test
  void publishMessageWithError() throws Exception {
    PublishPrivateMessageRequest request =
        PublishPrivateMessageRequest.builder().channelId("invalid id").message(" ").build();
    // test 400
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/channel/private/publishMessage")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.errors.channelId").exists())
        .andExpect(jsonPath("$.errors.message").exists())
        .andExpect(status().isBadRequest());
    // test 404
    String id = UUID.randomUUID().toString();
    while (Objects.equals(channel.getId().toString(), id)) id = UUID.randomUUID().toString();
    request = PublishPrivateMessageRequest.builder().channelId(id).message("msg").build();

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/channel/private/publishMessage")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.message").exists())
        .andExpect(status().isNotFound());
    // test 403
    request =
        PublishPrivateMessageRequest.builder()
            .channelId(channel.getId().toString())
            .message("msg")
            .build();
    // override mock login
    mockAuthUtil.when(AuthUtil::currentUserDetail).thenReturn(new UserDetail(otherUser));
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/channel/private/publishMessage")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.message").exists())
        .andExpect(status().isForbidden());
  }

  @Test
  void getMessages() throws Exception {
    // prepare
    List<PrivateMessageDto> messages = new ArrayList<>();
    for (int i = 0; i < 10; i++)
      messages.add(
          messageService.createMessage(
              user1.getId().toString(), channel.getId().toString(), "msg"));
    PageRequest request = PageRequest.builder().page(0).size(messages.size()).build();
    LinkedMultiValueMap<String, String> query = new LinkedMultiValueMap<>();
    query.add("page", request.getPage().toString());
    query.add("size", request.getSize().toString());
    // test success
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/api/channel/private/getAllMessages")
                    .params(query)
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
    SliceOfMessage<MessageDto> slice =
        objectMapper.readValue(result.getResponse().getContentAsString(), SliceOfMessage.class);
    assertEquals(request.getSize(), slice.getPageSize());
    assertEquals(request.getPage(), slice.getCurrentPage());
    assertEquals(request.getSize(), slice.getMessages().size());
    for (int i = 0; i < messages.size(); i++) {
      assertEquals(messages.get(i).getId(), slice.getMessages().get(i).getId());
    }
  }

  @Test
  void getMessagesWithError() throws Exception {
    // test 400
    PageRequest request = PageRequest.builder().page(-1).size(0).build();
    LinkedMultiValueMap<String, String> query = new LinkedMultiValueMap<>();
    query.add("page", request.getPage().toString());
    query.add("size", request.getSize().toString());
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/channel/private/getAllMessages")
                .params(query)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.errors.page").exists())
        .andExpect(jsonPath("$.errors.size").exists())
        .andExpect(status().isBadRequest());
  }

  @Test
  void getMessagesSince() throws Exception {
    // prepare
    List<PrivateMessageDto> messages = new ArrayList<>();
    for (int i = 0; i < 10; i++)
      messageService.createMessage(user1.getId().toString(), channel.getId().toString(), "msg");
    Instant since = Instant.now();
    Thread.sleep(1000);
    for (int i = 0; i < 10; i++)
      messages.add(
          messageService.createMessage(
              user1.getId().toString(), channel.getId().toString(), "msg"));
    // test success
    GetPrivateMessageSinceRequest request =
        GetPrivateMessageSinceRequest.builder()
            .since(since)
            .pageRequest(PageRequest.builder().page(0).size(messages.size()).build())
            .build();
    LinkedMultiValueMap<String, String> query = new LinkedMultiValueMap<>();
    query.add("pageRequest.page", request.getPageRequest().getPage().toString());
    query.add("pageRequest.size", request.getPageRequest().getSize().toString());
    query.add("since", request.getSince().toString());
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/api/channel/private/getMessagesSince")
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
      assertEquals(messages.get(0).getId(), slice.getMessages().get(0).getId());
    }
  }

  @Test
  void getMessagesSinceWithError() throws Exception {
    // test 400
    GetPrivateMessageSinceRequest request =
        GetPrivateMessageSinceRequest.builder()
            .pageRequest(PageRequest.builder().page(-1).size(0).build())
            .build();
    LinkedMultiValueMap<String, String> query = new LinkedMultiValueMap<>();
    query.add("pageRequest.page", request.getPageRequest().getPage().toString());
    query.add("pageRequest.size", request.getPageRequest().getSize().toString());
    query.add("since", "invalid time !");

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/channel/private/getMessagesSince")
                .params(query)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.errors.['pageRequest.page']").exists())
        .andExpect(jsonPath("$.errors.['pageRequest.size']").exists())
        .andExpect(jsonPath("$.errors.since").exists())
        .andExpect(status().isBadRequest());
  }

  @Test
  void create() throws Exception {
    // test success
    CreatePrivateChannelRequest request =
        CreatePrivateChannelRequest.builder().targetUserId(otherUser.getId().toString()).build();
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/channel/private/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
    PrivateChannelProfile returned =
        objectMapper.readValue(
            result.getResponse().getContentAsString(), PrivateChannelProfile.class);
    PrivateChannelProfile real =
        channelService.getChannelProfile(request.getTargetUserId(), returned.getId());
    assertEquals(real, returned);
  }

  @Test
  void createWithError() throws Exception {
    // test 400
    CreatePrivateChannelRequest request =
        CreatePrivateChannelRequest.builder().targetUserId("invalid id").build();
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/channel/private/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.errors.targetUserId").exists())
            .andExpect(status().isBadRequest())
            .andReturn();
    // test 403
    request =
        CreatePrivateChannelRequest.builder().targetUserId(otherUser.getId().toString()).build();
    for (Throwable e :
        Arrays.asList(new AlreadyExist(""), new InvalidOperation(""), new UserDoesNotExist(""))) {
      Mockito.doThrow(e)
          .when(channelService)
          .createChannelBetween(user1.getId().toString(), request.getTargetUserId());
      mockMvc
          .perform(
              MockMvcRequestBuilders.post("/api/channel/private/create")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
                  .accept(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.message").exists())
          .andExpect(status().isForbidden())
          .andReturn();
    }
  }

  @Test
  void list() throws Exception {
    // prepare
    PageRequest request = PageRequest.builder().size(1).page(0).build();
    SliceList<PrivateChannelProfile> profiles =
        channelService.getAllChannels(user1.getId().toString(), request);
    // test success
    LinkedMultiValueMap<String, String> query = new LinkedMultiValueMap<>();
    query.add("page", request.getPage().toString());
    query.add("size", request.getSize().toString());
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/api/channel/private/list")
                    .params(query)
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
    SliceOfChannel slice =
        objectMapper.readValue(result.getResponse().getContentAsString(), SliceOfChannel.class);
    assertEquals(profiles.getList(), slice.getChannels());
  }

  @Test
  void listWithError() throws Exception {
    // test 400
    PageRequest request = PageRequest.builder().size(0).page(-1).build();
    LinkedMultiValueMap<String, String> query = new LinkedMultiValueMap<>();
    query.add("page", request.getPage().toString());
    query.add("size", request.getSize().toString());

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/channel/private/list")
                .params(query)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.errors.page").exists())
        .andExpect(jsonPath("$.errors.size").exists())
        .andExpect(status().isBadRequest());
  }

  @Test
  void profile() throws Exception {
    // test success
    GetChannelProfileRequest request =
        GetChannelProfileRequest.builder().channelId(channel.getId().toString()).build();
    LinkedMultiValueMap<String, String> query = new LinkedMultiValueMap<>();
    query.add("channelId", request.getChannelId());

    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/api/channel/private/profile")
                    .params(query)
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
    PrivateChannelProfile returned =
        objectMapper.readValue(
            result.getResponse().getContentAsString(), PrivateChannelProfile.class);
    PrivateChannelProfile real =
        channelService.getChannelProfile(user1.getId().toString(), returned.getId());
    assertEquals(real, returned);
  }

  @Test
  void profileWithError() throws Exception {
    GetChannelProfileRequest request =
        GetChannelProfileRequest.builder().channelId("invalid id").build();
    // test 400
    LinkedMultiValueMap<String, String> query = new LinkedMultiValueMap<>();
    query.add("channelId", request.getChannelId());

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/channel/private/profile")
                .params(query)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.errors.channelId").exists())
        .andExpect(status().isBadRequest());
    // test 404
    String id = UUID.randomUUID().toString();
    while (Objects.equals(channel.getId().toString(), id)) id = UUID.randomUUID().toString();
    request = GetChannelProfileRequest.builder().channelId(id).build();
    query = new LinkedMultiValueMap<>();
    query.add("channelId", request.getChannelId());

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/channel/private/profile")
                .params(query)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.message").exists())
        .andExpect(status().isNotFound());
    // test 403
    request = GetChannelProfileRequest.builder().channelId(channel.getId().toString()).build();
    query = new LinkedMultiValueMap<>();
    query.add("channelId", request.getChannelId());
    // override mock login
    mockAuthUtil.when(AuthUtil::currentUserDetail).thenReturn(new UserDetail(otherUser));
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/channel/private/profile")
                .params(query)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.message").exists())
        .andExpect(status().isForbidden());
  }
}
