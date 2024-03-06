package com.joejoe2.chat.controller;

import com.joejoe2.chat.controller.constraint.auth.AuthenticatedApi;
import com.joejoe2.chat.data.*;
import com.joejoe2.chat.data.channel.PageOfChannel;
import com.joejoe2.chat.data.channel.profile.PublicChannelProfile;
import com.joejoe2.chat.data.channel.request.ChannelPageRequest;
import com.joejoe2.chat.data.channel.request.ChannelPageRequestWithSince;
import com.joejoe2.chat.data.channel.request.ChannelRequest;
import com.joejoe2.chat.data.channel.request.CreateChannelByNameRequest;
import com.joejoe2.chat.data.channel.request.SubscribeChannelRequest;
import com.joejoe2.chat.data.message.PublicMessageDto;
import com.joejoe2.chat.data.message.SliceOfMessage;
import com.joejoe2.chat.data.message.request.PublishMessageRequest;
import com.joejoe2.chat.exception.AlreadyExist;
import com.joejoe2.chat.exception.ChannelDoesNotExist;
import com.joejoe2.chat.exception.UserDoesNotExist;
import com.joejoe2.chat.service.channel.PublicChannelService;
import com.joejoe2.chat.service.message.PublicMessageService;
import com.joejoe2.chat.utils.AuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping(path = "/api/channel/public")
public class PublicChannelController {
  private final PublicChannelService channelService;
  private final PublicMessageService messageService;

  public PublicChannelController(
      PublicChannelService channelService, PublicMessageService messageService) {
    this.channelService = channelService;
    this.messageService = messageService;
  }

  @Operation(summary = "publish message to public channel")
  @AuthenticatedApi
  @SecurityRequirement(name = "jwt")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "404",
            description = "target channel is not exist",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorMessageResponse.class))),
        @ApiResponse(
            responseCode = "200",
            description = "publish message to target channel",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PublicMessageDto.class))),
      })
  @RequestMapping(path = "/publishMessage", method = RequestMethod.POST)
  public ResponseEntity<Object> publishMessage(@Valid @RequestBody PublishMessageRequest request)
      throws UserDoesNotExist {
    try {
      PublicMessageDto message =
          messageService.createMessage(
              AuthUtil.currentUserDetail().getId(), request.getChannelId(), request.getMessage());
      messageService.deliverMessage(message);
      return ResponseEntity.ok(message);
    } catch (ChannelDoesNotExist e) {
      return new ResponseEntity<>(new ErrorMessageResponse(e.getMessage()), HttpStatus.NOT_FOUND);
    }
  }

  @Operation(summary = "get all messages in public channel")
  @AuthenticatedApi
  @SecurityRequirement(name = "jwt")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "404",
            description = "target channel is not exist",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorMessageResponse.class))),
        @ApiResponse(
            responseCode = "200",
            description = "messages in target channel",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = SliceOfMessage.class))),
      })
  @RequestMapping(path = "/getAllMessages", method = RequestMethod.GET)
  public ResponseEntity<Object> getMessages(@ParameterObject @Valid ChannelPageRequest request) {
    try {
      SliceList<PublicMessageDto> sliceList =
          messageService.getAllMessages(request.getChannelId(), request.getPageRequest());
      return ResponseEntity.ok(new SliceOfMessage<>(sliceList));
    } catch (ChannelDoesNotExist e) {
      return new ResponseEntity<>(new ErrorMessageResponse(e.getMessage()), HttpStatus.NOT_FOUND);
    }
  }

  @Operation(summary = "get all messages in public channel since")
  @AuthenticatedApi
  @SecurityRequirement(name = "jwt")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "404",
            description = "target channel is not exist",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorMessageResponse.class))),
        @ApiResponse(
            responseCode = "200",
            description = "messages in target channel",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = SliceOfMessage.class))),
      })
  @RequestMapping(path = "/getMessagesSince", method = RequestMethod.GET)
  public ResponseEntity<Object> getMessagesSince(
      @ParameterObject @Valid ChannelPageRequestWithSince request) {
    try {
      SliceList<PublicMessageDto> sliceList =
          messageService.getAllMessages(
              request.getChannelId(), request.getSince(), request.getPageRequest());
      return ResponseEntity.ok(new SliceOfMessage<>(sliceList));
    } catch (ChannelDoesNotExist e) {
      return new ResponseEntity<>(new ErrorMessageResponse(e.getMessage()), HttpStatus.NOT_FOUND);
    }
  }

  @Operation(summary = "create public channel")
  @AuthenticatedApi
  @SecurityRequirement(name = "jwt")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "403",
            description = "target channel is already exist",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorMessageResponse.class))),
        @ApiResponse(
            responseCode = "200",
            description = "create public channel",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PublicChannelProfile.class))),
      })
  @RequestMapping(path = "/create", method = RequestMethod.POST)
  public ResponseEntity<Object> create(@Valid @RequestBody CreateChannelByNameRequest request) {
    try {
      return ResponseEntity.ok(channelService.createChannel(request.getChannelName()));
    } catch (AlreadyExist e) {
      return new ResponseEntity<>(new ErrorMessageResponse(e.getMessage()), HttpStatus.FORBIDDEN);
    }
  }

  @Operation(summary = "get profiles of all public channels")
  @AuthenticatedApi
  @SecurityRequirement(name = "jwt")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "profiles of channel",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PageOfChannel.class))),
      })
  @RequestMapping(path = "/list", method = RequestMethod.GET)
  public ResponseEntity<Object> list(@ParameterObject @Valid PageRequest request) {
    PageList<PublicChannelProfile> pageList = channelService.getAllChannels(request);
    return ResponseEntity.ok(new PageOfChannel(pageList));
  }

  @Operation(summary = "get profile of public channel")
  @AuthenticatedApi
  @SecurityRequirement(name = "jwt")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "404",
            description = "target channel is not exist",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorMessageResponse.class))),
        @ApiResponse(
            responseCode = "200",
            description = "profile of channel",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PublicChannelProfile.class))),
      })
  @RequestMapping(path = "/profile", method = RequestMethod.GET)
  public ResponseEntity<Object> profile(@ParameterObject @Valid ChannelRequest request) {
    try {
      PublicChannelProfile profile = channelService.getChannelProfile(request.getChannelId());
      return ResponseEntity.ok(profile);
    } catch (ChannelDoesNotExist e) {
      return new ResponseEntity<>(new ErrorMessageResponse(e.getMessage()), HttpStatus.NOT_FOUND);
    }
  }

  @Operation(summary = "subscribe to public channel")
  @AuthenticatedApi
  @SecurityRequirement(name = "jwt-in-query")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "404",
            description = "target channel is not exist",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorMessageResponse.class))),
        @ApiResponse(
            responseCode = "200",
            description =
                "subscribe to public channel via SSE, "
                    + "you will receive messages in data event like below",
            content =
                @Content(
                    mediaType = "application/json",
                    array =
                        @ArraySchema(schema = @Schema(implementation = PublicMessageDto.class)))),
      })
  @RequestMapping(path = "/subscribe", method = RequestMethod.GET)
  public Object subscribe(@ParameterObject @Valid SubscribeChannelRequest request) {
    try {
      return channelService.subscribe(request.getChannelId());
    } catch (ChannelDoesNotExist e) {
      return new ResponseEntity<>(new ErrorMessageResponse(e.getMessage()), HttpStatus.NOT_FOUND);
    }
  }
}
