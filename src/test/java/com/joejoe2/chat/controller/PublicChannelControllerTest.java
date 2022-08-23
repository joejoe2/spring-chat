package com.joejoe2.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joejoe2.chat.data.PageList;
import com.joejoe2.chat.data.PageRequest;
import com.joejoe2.chat.data.SliceList;
import com.joejoe2.chat.data.UserDetail;
import com.joejoe2.chat.data.channel.profile.PublicChannelProfile;
import com.joejoe2.chat.data.channel.request.CreatePublicChannelRequest;
import com.joejoe2.chat.data.channel.request.GetChannelProfileRequest;
import com.joejoe2.chat.data.message.PublicMessageDto;
import com.joejoe2.chat.data.message.request.GetAllPublicMessageRequest;
import com.joejoe2.chat.data.message.request.GetPublicMessageSinceRequest;
import com.joejoe2.chat.data.message.request.PublishPublicMessageRequest;
import com.joejoe2.chat.exception.AlreadyExist;
import com.joejoe2.chat.exception.ChannelDoesNotExist;
import com.joejoe2.chat.models.PublicChannel;
import com.joejoe2.chat.models.User;
import com.joejoe2.chat.repository.channel.PublicChannelRepository;
import com.joejoe2.chat.repository.user.UserRepository;
import com.joejoe2.chat.service.channel.PublicChannelService;
import com.joejoe2.chat.service.message.PublicMessageService;
import com.joejoe2.chat.utils.AuthUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.LinkedMultiValueMap;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicChannelControllerTest {
    MockedStatic<AuthUtil> mockAuthUtil;
    User user;
    PublicChannel channel;
    @Autowired
    UserRepository userRepository;
    @Autowired
    PublicChannelRepository channelRepository;
    @Autowired
    MockMvc mockMvc;
    @MockBean
    PublicMessageService messageService;
    @MockBean
    PublicChannelService channelService;

    ObjectMapper objectMapper=new ObjectMapper();

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).userName("test").build();
        userRepository.save(user);
        channel = new PublicChannel();
        channel.setName("test");
        channelRepository.save(channel);
        //mock login
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
    void publishMessage() throws Exception{
        PublishPublicMessageRequest request=PublishPublicMessageRequest
                .builder().channelId("invalid id").message(" ").build();
        //test 400
        mockMvc.perform(MockMvcRequestBuilders.post("/api/channel/public/publishMessage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errors.channelId").exists())
                .andExpect(jsonPath("$.errors.message").exists())
                .andExpect(status().isBadRequest());
        //test 404
        String id = UUID.randomUUID().toString();
        while (Objects.equals(channel.getId().toString(), id))
            id = UUID.randomUUID().toString();
        request = PublishPublicMessageRequest
                .builder().channelId(id).message("msg").build();

        Mockito.doThrow(new ChannelDoesNotExist("")).when(messageService)
                .createMessage(user.getId().toString(), request.getChannelId(), request.getMessage());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/channel/public/publishMessage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(status().isNotFound());
        Mockito.verify(messageService, Mockito.never()).deliverMessage(Mockito.any());
        //test success
        request = PublishPublicMessageRequest
                .builder().channelId(channel.getId().toString()).message("msg").build();

        Mockito.doReturn(new PublicMessageDto()).when(messageService)
                .createMessage(user.getId().toString(), request.getChannelId(), request.getMessage());
        Mockito.doNothing().when(messageService).deliverMessage(Mockito.any());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/channel/public/publishMessage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Mockito.verify(messageService, Mockito.times(1))
                .createMessage(user.getId().toString(), request.getChannelId(), request.getMessage());
        Mockito.verify(messageService, Mockito.times(1))
                .deliverMessage(Mockito.any());
    }

    @Test
    void getMessages() throws Exception{
        GetAllPublicMessageRequest request = GetAllPublicMessageRequest.builder()
                .channelId("invalid id")
                .pageRequest(PageRequest.builder().page(-1).size(0).build())
                .build();
        LinkedMultiValueMap<String, String> query = new LinkedMultiValueMap<>();
        query.add("channelId", request.getChannelId());
        query.add("pageRequest.page", request.getPageRequest().getPage().toString());
        query.add("pageRequest.size", request.getPageRequest().getSize().toString());
        //test 400
        mockMvc.perform(MockMvcRequestBuilders.get("/api/channel/public/getAllMessages")
                        .params(query)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errors.channelId").exists())
                .andExpect(jsonPath("$.errors.['pageRequest.page']").exists())
                .andExpect(jsonPath("$.errors.['pageRequest.size']").exists())
                .andExpect(status().isBadRequest());
        //test 404
        String id = UUID.randomUUID().toString();
        while (Objects.equals(channel.getId().toString(), id))
            id = UUID.randomUUID().toString();
        request = GetAllPublicMessageRequest.builder()
                .pageRequest(PageRequest.builder().page(0).size(1).build())
                .channelId(id)
                .build();
        query = new LinkedMultiValueMap<>();
        query.add("channelId", request.getChannelId());
        query.add("pageRequest.page", request.getPageRequest().getPage().toString());
        query.add("pageRequest.size", request.getPageRequest().getSize().toString());
        Mockito.doThrow(new ChannelDoesNotExist("")).when(messageService)
                .getAllMessages(request.getChannelId(), request.getPageRequest());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/channel/public/getAllMessages")
                        .params(query)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(status().isNotFound());
        //test success
        request = GetAllPublicMessageRequest.builder()
                .pageRequest(PageRequest.builder().page(0).size(1).build())
                .channelId(channel.getId().toString())
                .build();
        query = new LinkedMultiValueMap<>();
        query.add("channelId", request.getChannelId());
        query.add("pageRequest.page", request.getPageRequest().getPage().toString());
        query.add("pageRequest.size", request.getPageRequest().getSize().toString());
        Mockito.doReturn(new SliceList<>(0, 1, List.of(new PublicMessageDto()), true))
                .when(messageService).getAllMessages(request.getChannelId(), request.getPageRequest());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/channel/public/getAllMessages")
                        .params(query)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void getMessagesSince() throws Exception{
        GetPublicMessageSinceRequest request = GetPublicMessageSinceRequest.builder()
                .channelId("invalid id")
                .pageRequest(PageRequest.builder().page(-1).size(0).build())
                .since(Instant.now())
                .build();
        LinkedMultiValueMap<String, String> query = new LinkedMultiValueMap<>();
        query.add("channelId", request.getChannelId());
        query.add("pageRequest.page", request.getPageRequest().getPage().toString());
        query.add("pageRequest.size", request.getPageRequest().getSize().toString());
        query.add("since", request.getSince().toString()+"invalid time");
        //test 400
        mockMvc.perform(MockMvcRequestBuilders.get("/api/channel/public/getMessagesSince")
                        .params(query)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errors.channelId").exists())
                .andExpect(jsonPath("$.errors.['pageRequest.page']").exists())
                .andExpect(jsonPath("$.errors.['pageRequest.size']").exists())
                .andExpect(jsonPath("$.errors.since").exists())
                .andExpect(status().isBadRequest());
        //test 404
        String id = UUID.randomUUID().toString();
        while (Objects.equals(channel.getId().toString(), id))
            id = UUID.randomUUID().toString();
        request = GetPublicMessageSinceRequest.builder()
                .pageRequest(PageRequest.builder().page(0).size(1).build())
                .channelId(id)
                .since(Instant.now())
                .build();
        query = new LinkedMultiValueMap<>();
        query.add("channelId", request.getChannelId());
        query.add("pageRequest.page", request.getPageRequest().getPage().toString());
        query.add("pageRequest.size", request.getPageRequest().getSize().toString());
        query.add("since", request.getSince().toString());
        Mockito.doThrow(new ChannelDoesNotExist("")).when(messageService)
                .getAllMessages(request.getChannelId(), request.getSince(), request.getPageRequest());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/channel/public/getMessagesSince")
                        .params(query)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(status().isNotFound());
        //test success
        request = GetPublicMessageSinceRequest.builder()
                .pageRequest(PageRequest.builder().page(0).size(1).build())
                .channelId(channel.getId().toString())
                .since(Instant.now())
                .build();
        query = new LinkedMultiValueMap<>();
        query.add("channelId", request.getChannelId());
        query.add("pageRequest.page", request.getPageRequest().getPage().toString());
        query.add("pageRequest.size", request.getPageRequest().getSize().toString());
        query.add("since", request.getSince().toString());
        Mockito.doReturn(new SliceList<>(0, 1, List.of(new PublicMessageDto()), true))
                .when(messageService).getAllMessages(request.getChannelId(), request.getSince(), request.getPageRequest());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/channel/public/getMessagesSince")
                        .params(query)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void create() throws Exception{
        CreatePublicChannelRequest request = CreatePublicChannelRequest.builder()
                .channelName("invalid name !!!")
                .build();
        //test 400
        mockMvc.perform(MockMvcRequestBuilders.post("/api/channel/public/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errors.channelName").exists())
                .andExpect(status().isBadRequest());
        //test 403
        request = CreatePublicChannelRequest.builder()
                .channelName("duplicate")
                .build();
        Mockito.doThrow(new AlreadyExist("")).when(channelService)
                .createChannel(request.getChannelName());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/channel/public/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(status().isForbidden());
        //test success
        request = CreatePublicChannelRequest.builder()
                .channelName("test")
                .build();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/channel/public/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void list() throws Exception{
        PageRequest request=PageRequest.builder()
                .page(-1).size(0)
                .build();
        //test 400
        LinkedMultiValueMap<String, String> query = new LinkedMultiValueMap<>();
        query.add("page", request.getPage().toString());
        query.add("size", request.getSize().toString());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/channel/public/list")
                        .params(query)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errors.page").exists())
                .andExpect(jsonPath("$.errors.size").exists())
                .andExpect(status().isBadRequest());
        //test success
        request=PageRequest.builder()
                .page(0).size(1)
                .build();
        query = new LinkedMultiValueMap<>();
        query.add("page", request.getPage().toString());
        query.add("size", request.getSize().toString());
        List<PublicChannelProfile> profiles=List.of(new PublicChannelProfile(channel));
        Mockito.doReturn(new PageList<>(1, request.getPage(),
                1, request.getSize(), profiles))
                .when(channelService).getAllChannels(request);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/channel/public/list")
                        .params(query)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.channels").isArray())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.currentPage").value(request.getPage()))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.pageSize").value(request.getSize()))
                .andExpect(status().isOk());
    }

    @Test
    void profile() throws Exception{
        GetChannelProfileRequest request = GetChannelProfileRequest.builder()
                .channelId("invalid id")
                .build();
        //test 400
        LinkedMultiValueMap<String, String> query = new LinkedMultiValueMap<>();
        query.add("channelId", request.getChannelId());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/channel/public/profile")
                        .params(query)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errors.channelId").exists())
                .andExpect(status().isBadRequest());
        //test 404
        String id = UUID.randomUUID().toString();
        while (Objects.equals(channel.getId().toString(), id))
            id = UUID.randomUUID().toString();
        request = GetChannelProfileRequest.builder()
                .channelId(id)
                .build();
        query = new LinkedMultiValueMap<>();
        query.add("channelId", request.getChannelId());
        Mockito.doThrow(new ChannelDoesNotExist(""))
                .when(channelService).getChannelProfile(request.getChannelId());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/channel/public/profile")
                        .params(query)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(status().isNotFound());
        //test success
        request = GetChannelProfileRequest.builder()
                .channelId(channel.getId().toString())
                .build();
        query = new LinkedMultiValueMap<>();
        query.add("channelId", request.getChannelId());
        Mockito.doReturn(new PublicChannelProfile(channel))
                .when(channelService).getChannelProfile(request.getChannelId());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/channel/public/profile")
                        .params(query)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void subscribe() {
    }
}