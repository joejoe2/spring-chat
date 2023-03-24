package com.joejoe2.chat.controller;

import com.joejoe2.chat.controller.constraint.auth.AuthenticatedApi;
import com.joejoe2.chat.data.ErrorMessageResponse;
import com.joejoe2.chat.data.PageList;
import com.joejoe2.chat.data.PageRequest;
import com.joejoe2.chat.data.SliceList;
import com.joejoe2.chat.data.channel.PageOfChannel;
import com.joejoe2.chat.data.channel.profile.PublicChannelProfile;
import com.joejoe2.chat.data.channel.request.CreatePublicChannelRequest;
import com.joejoe2.chat.data.channel.request.GetChannelProfileRequest;
import com.joejoe2.chat.data.channel.request.SubscribePublicChannelRequest;
import com.joejoe2.chat.data.message.PublicMessageDto;
import com.joejoe2.chat.data.message.SliceOfMessage;
import com.joejoe2.chat.data.message.request.GetAllPublicMessageRequest;
import com.joejoe2.chat.data.message.request.GetPublicMessageSinceRequest;
import com.joejoe2.chat.data.message.request.PublishPublicMessageRequest;
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
import javax.validation.Valid;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping(path = "/api/channel/public")
public class PublicChannelController {
  @Autowired PublicChannelService channelService;
  @Autowired PublicMessageService messageService;

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
  public ResponseEntity<Object> publishMessage(
      @Valid @RequestBody PublishPublicMessageRequest request) throws UserDoesNotExist {
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
  public ResponseEntity<Object> getMessages(
      @ParameterObject @Valid GetAllPublicMessageRequest request) {
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
      @ParameterObject @Valid GetPublicMessageSinceRequest request) {
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
  public ResponseEntity<Object> create(@Valid @RequestBody CreatePublicChannelRequest request) {
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
  public ResponseEntity<Object> profile(@ParameterObject @Valid GetChannelProfileRequest request) {
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
  public Object subscribe(@ParameterObject @Valid SubscribePublicChannelRequest request) {
    try {
      return channelService.subscribe(request.getChannelId());
    } catch (ChannelDoesNotExist e) {
      return new ResponseEntity<>(new ErrorMessageResponse(e.getMessage()), HttpStatus.NOT_FOUND);
    }
  }
}
