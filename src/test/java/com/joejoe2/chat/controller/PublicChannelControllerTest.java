package com.joejoe2.chat.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joejoe2.chat.TestContext;
import com.joejoe2.chat.data.PageRequest;
import com.joejoe2.chat.data.PageRequestWithSince;
import com.joejoe2.chat.data.UserDetail;
import com.joejoe2.chat.data.channel.PageOfChannel;
import com.joejoe2.chat.data.channel.profile.PublicChannelProfile;
import com.joejoe2.chat.data.channel.request.ChannelPageRequest;
import com.joejoe2.chat.data.channel.request.ChannelPageRequestWithSince;
import com.joejoe2.chat.data.channel.request.ChannelRequest;
import com.joejoe2.chat.data.channel.request.CreateChannelByNameRequest;
import com.joejoe2.chat.data.message.MessageDto;
import com.joejoe2.chat.data.message.PublicMessageDto;
import com.joejoe2.chat.data.message.SliceOfMessage;
import com.joejoe2.chat.data.message.request.PublishMessageRequest;
import com.joejoe2.chat.models.PublicChannel;
import com.joejoe2.chat.models.User;
import com.joejoe2.chat.repository.channel.PublicChannelRepository;
import com.joejoe2.chat.repository.message.PublicMessageRepository;
import com.joejoe2.chat.repository.user.UserRepository;
import com.joejoe2.chat.service.channel.PublicChannelService;
import com.joejoe2.chat.service.message.PublicMessageService;
import com.joejoe2.chat.utils.AuthUtil;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
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
class PublicChannelControllerTest {
  MockedStatic<AuthUtil> mockAuthUtil;
  User user;
  PublicChannel channel;
  @Autowired UserRepository userRepository;
  @Autowired PublicChannelRepository channelRepository;
  @Autowired PublicMessageRepository messageRepository;
  @Autowired MockMvc mockMvc;
  @SpyBean PublicMessageService messageService;
  @SpyBean PublicChannelService channelService;

  @Autowired ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    user = User.builder().id(UUID.randomUUID()).userName("test").build();
    userRepository.save(user);
    channel = new PublicChannel();
    channel.setName("test");
    channelRepository.save(channel);
    // mock login
    mockAuthUtil = Mockito.mockStatic(AuthUtil.class);
    mockAuthUtil.when(AuthUtil::isAuthenticated).thenReturn(true);
    mockAuthUtil.when(AuthUtil::currentUserDetail).thenReturn(new UserDetail(user));
  }

  @AfterEach
  void tearDown() {
    channelRepository.deleteAll();
    userRepository.deleteAll();
    mockAuthUtil.close();
  }

  @Test
  void publishMessage() throws Exception {
    PublishMessageRequest request =
        PublishMessageRequest.builder()
            .channelId(channel.getId().toString())
            .message("msg")
            .build();
    // test success
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/channel/public/publishMessage")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
    PublicMessageDto message =
        messageService
            .getAllMessages(
                channel.getId().toString(), PageRequest.builder().page(0).size(1).build())
            .getList()
            .get(0);
    assertEquals(
        message,
        objectMapper.readValue(result.getResponse().getContentAsString(), PublicMessageDto.class));
    Thread.sleep(1000);
    Mockito.verify(messageService, Mockito.times(1)).deliverMessage(Mockito.any());
  }

  @Test
  void publishMessageWithError() throws Exception {
    PublishMessageRequest request =
        PublishMessageRequest.builder().channelId("invalid id").message(" ").build();
    // test 400
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/channel/public/publishMessage")
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
            MockMvcRequestBuilders.post("/api/channel/public/publishMessage")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.message").exists())
        .andExpect(status().isNotFound());
  }

  @Test
  void getMessages() throws Exception {
    // prepare
    List<PublicMessageDto> messages = new ArrayList<>();
    for (int i = 0; i < 10; i++)
      messages.add(
          messageService.createMessage(user.getId().toString(), channel.getId().toString(), "msg"));
    // test success
    ChannelPageRequest request =
        ChannelPageRequest.builder()
            .pageRequest(PageRequest.builder().page(0).size(messages.size()).build())
            .channelId(channel.getId().toString())
            .build();
    LinkedMultiValueMap<String, String> query = new LinkedMultiValueMap<>();
    query.add("channelId", request.getChannelId());
    query.add("pageRequest.page", request.getPageRequest().getPage().toString());
    query.add("pageRequest.size", request.getPageRequest().getSize().toString());
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/api/channel/public/getAllMessages")
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
    ChannelPageRequest request =
        ChannelPageRequest.builder()
            .channelId("invalid id")
            .pageRequest(PageRequest.builder().page(-1).size(0).build())
            .build();
    LinkedMultiValueMap<String, String> query = new LinkedMultiValueMap<>();
    query.add("channelId", request.getChannelId());
    query.add("pageRequest.page", request.getPageRequest().getPage().toString());
    query.add("pageRequest.size", request.getPageRequest().getSize().toString());
    // test 400
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/channel/public/getAllMessages")
                .params(query)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.errors.channelId").exists())
        .andExpect(jsonPath("$.errors.['pageRequest.page']").exists())
        .andExpect(jsonPath("$.errors.['pageRequest.size']").exists())
        .andExpect(status().isBadRequest());
    // test 404
    String id = UUID.randomUUID().toString();
    while (Objects.equals(channel.getId().toString(), id)) id = UUID.randomUUID().toString();
    request =
        ChannelPageRequest.builder()
            .pageRequest(PageRequest.builder().page(0).size(1).build())
            .channelId(id)
            .build();
    query = new LinkedMultiValueMap<>();
    query.add("channelId", request.getChannelId());
    query.add("pageRequest.page", request.getPageRequest().getPage().toString());
    query.add("pageRequest.size", request.getPageRequest().getSize().toString());
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/channel/public/getAllMessages")
                .params(query)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.message").exists())
        .andExpect(status().isNotFound());
  }

  @Test
  void getMessagesSince() throws Exception {
    // prepare
    List<PublicMessageDto> messages = new ArrayList<>();
    for (int i = 0; i < 10; i++)
      messageService.createMessage(user.getId().toString(), channel.getId().toString(), "msg");
    Instant since = Instant.now();
    Thread.sleep(1000);
    for (int i = 0; i < 10; i++)
      messages.add(
          messageService.createMessage(user.getId().toString(), channel.getId().toString(), "msg"));
    // test success
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
                MockMvcRequestBuilders.get("/api/channel/public/getMessagesSince")
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
  void getMessagesSinceWithError() throws Exception {
    ChannelPageRequestWithSince request =
        ChannelPageRequestWithSince.builder()
            .channelId("invalid id")
            .pageRequest(PageRequest.builder().page(-1).size(0).build())
            .since(Instant.now())
            .build();
    LinkedMultiValueMap<String, String> query = new LinkedMultiValueMap<>();
    query.add("channelId", request.getChannelId());
    query.add("pageRequest.page", request.getPageRequest().getPage().toString());
    query.add("pageRequest.size", request.getPageRequest().getSize().toString());
    query.add("since", request.getSince().toString() + "invalid time");
    // test 400
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/channel/public/getMessagesSince")
                .params(query)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.errors.channelId").exists())
        .andExpect(jsonPath("$.errors.['pageRequest.page']").exists())
        .andExpect(jsonPath("$.errors.['pageRequest.size']").exists())
        .andExpect(jsonPath("$.errors.since").exists())
        .andExpect(status().isBadRequest());
    // test 404
    String id = UUID.randomUUID().toString();
    while (Objects.equals(channel.getId().toString(), id)) id = UUID.randomUUID().toString();
    request =
        ChannelPageRequestWithSince.builder()
            .pageRequest(PageRequest.builder().page(0).size(1).build())
            .channelId(id)
            .since(Instant.now())
            .build();
    query = new LinkedMultiValueMap<>();
    query.add("channelId", request.getChannelId());
    query.add("pageRequest.page", request.getPageRequest().getPage().toString());
    query.add("pageRequest.size", request.getPageRequest().getSize().toString());
    query.add("since", request.getSince().toString());

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/channel/public/getMessagesSince")
                .params(query)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.message").exists())
        .andExpect(status().isNotFound());
  }

  @Test
  void create() throws Exception {
    // test success
    CreateChannelByNameRequest request =
        CreateChannelByNameRequest.builder().channelName("create").build();

    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/channel/public/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
    assertEquals(
        new PublicChannelProfile(channelRepository.findByName(request.getChannelName()).get()),
        objectMapper.readValue(
            result.getResponse().getContentAsString(), PublicChannelProfile.class));
  }

  @Test
  void createWithError() throws Exception {
    CreateChannelByNameRequest request =
        CreateChannelByNameRequest.builder().channelName("invalid name !!!").build();
    // test 400
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/channel/public/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.errors.channelName").exists())
        .andExpect(status().isBadRequest());
    // test 403
    request = CreateChannelByNameRequest.builder().channelName(channel.getName()).build();

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/channel/public/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.message").exists())
        .andExpect(status().isForbidden());
  }

  @Test
  void list() throws Exception {
    // prepare
    List<PublicChannelProfile> profiles = List.of(new PublicChannelProfile(channel));
    // test success
    PageRequest request = PageRequest.builder().page(0).size(profiles.size()).build();
    LinkedMultiValueMap<String, String> query = new LinkedMultiValueMap<>();
    query.add("page", request.getPage().toString());
    query.add("size", request.getSize().toString());

    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/api/channel/public/list")
                    .params(query)
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.channels").isArray())
            .andExpect(jsonPath("$.totalItems").value(request.getSize()))
            .andExpect(jsonPath("$.currentPage").value(request.getPage()))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.pageSize").value(request.getSize()))
            .andExpect(status().isOk())
            .andReturn();
    assertEquals(
        profiles.get(0),
        objectMapper
            .readValue(result.getResponse().getContentAsString(), PageOfChannel.class)
            .getChannels()
            .get(0));
  }

  @Test
  void listWithError() throws Exception {
    PageRequest request = PageRequest.builder().page(-1).size(0).build();
    // test 400
    LinkedMultiValueMap<String, String> query = new LinkedMultiValueMap<>();
    query.add("page", request.getPage().toString());
    query.add("size", request.getSize().toString());

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/channel/public/list")
                .params(query)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.errors.page").exists())
        .andExpect(jsonPath("$.errors.size").exists())
        .andExpect(status().isBadRequest());
  }

  @Test
  void profile() throws Exception {
    // test success
    ChannelRequest request = ChannelRequest.builder().channelId(channel.getId().toString()).build();
    LinkedMultiValueMap<String, String> query = new LinkedMultiValueMap<>();
    query.add("channelId", request.getChannelId());
    Mockito.doReturn(new PublicChannelProfile(channel))
        .when(channelService)
        .getChannelProfile(request.getChannelId());

    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/api/channel/public/profile")
                    .params(query)
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
    assertEquals(
        new PublicChannelProfile(channel),
        objectMapper.readValue(
            result.getResponse().getContentAsString(), PublicChannelProfile.class));
  }

  @Test
  void profileWithError() throws Exception {
    ChannelRequest request = ChannelRequest.builder().channelId("invalid id").build();
    // test 400
    LinkedMultiValueMap<String, String> query = new LinkedMultiValueMap<>();
    query.add("channelId", request.getChannelId());

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/channel/public/profile")
                .params(query)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.errors.channelId").exists())
        .andExpect(status().isBadRequest());
    // test 404
    String id = UUID.randomUUID().toString();
    while (Objects.equals(channel.getId().toString(), id)) id = UUID.randomUUID().toString();
    request = ChannelRequest.builder().channelId(id).build();
    query = new LinkedMultiValueMap<>();
    query.add("channelId", request.getChannelId());

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/channel/public/profile")
                .params(query)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.message").exists())
        .andExpect(status().isNotFound());
  }
}
