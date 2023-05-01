package com.joejoe2.chat.controller;

import com.joejoe2.chat.controller.constraint.auth.AuthenticatedApi;
import com.joejoe2.chat.data.ErrorMessageResponse;
import com.joejoe2.chat.data.PageRequestWithSince;
import com.joejoe2.chat.data.SliceList;
import com.joejoe2.chat.data.channel.SliceOfGroupChannel;
import com.joejoe2.chat.data.channel.profile.GroupChannelProfile;
import com.joejoe2.chat.data.channel.request.*;
import com.joejoe2.chat.data.channel.request.ChannelPageRequestWithSince;
import com.joejoe2.chat.data.message.GroupMessageDto;
import com.joejoe2.chat.data.message.SliceOfMessage;
import com.joejoe2.chat.data.message.request.PublishMessageRequest;
import com.joejoe2.chat.exception.ChannelDoesNotExist;
import com.joejoe2.chat.exception.InvalidOperation;
import com.joejoe2.chat.exception.UserDoesNotExist;
import com.joejoe2.chat.service.channel.GroupChannelService;
import com.joejoe2.chat.service.message.GroupMessageService;
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
@RequestMapping(path = "/api/channel/group")
public class GroupChannelController {
  @Autowired GroupChannelService channelService;
  @Autowired GroupMessageService messageService;

  @Operation(summary = "publish message to group channel")
  @AuthenticatedApi
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "403",
            description = "current user is not a member" + " of target channel",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorMessageResponse.class))),
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
                    schema = @Schema(implementation = GroupMessageDto.class))),
      })
  @RequestMapping(path = "/publishMessage", method = RequestMethod.POST)
  public ResponseEntity<Object> publishMessage(@RequestBody @Valid PublishMessageRequest request)
      throws UserDoesNotExist {
    try {
      GroupMessageDto message =
          messageService.createMessage(
              AuthUtil.currentUserDetail().getId(), request.getChannelId(), request.getMessage());
      messageService.deliverMessage(message);
      return ResponseEntity.ok(message);
    } catch (ChannelDoesNotExist e) {
      return new ResponseEntity<>(new ErrorMessageResponse(e.getMessage()), HttpStatus.NOT_FOUND);
    } catch (InvalidOperation e) {
      return new ResponseEntity<>(new ErrorMessageResponse(e.getMessage()), HttpStatus.FORBIDDEN);
    }
  }

  // todo: create channel

  @Operation(summary = "get all messages in group channel")
  @AuthenticatedApi
  @SecurityRequirement(name = "jwt")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "403",
            description = "current user is not a member" + " of target channel",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorMessageResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "target channel is not exist",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorMessageResponse.class))),
        @ApiResponse(
            responseCode = "200",
            description = "messages",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = SliceOfMessage.class))),
      })
  @RequestMapping(path = "/getMessagesSince", method = RequestMethod.GET)
  public ResponseEntity<Object> getMessagesSince(
      @ParameterObject @Valid ChannelPageRequestWithSince request) throws UserDoesNotExist {
    try {
      SliceList<GroupMessageDto> sliceList =
          messageService.getAllMessages(
              AuthUtil.currentUserDetail().getId(),
              request.getChannelId(),
              request.getSince(),
              request.getPageRequest());
      return ResponseEntity.ok(new SliceOfMessage<>(sliceList));
    } catch (ChannelDoesNotExist e) {
      return new ResponseEntity<>(new ErrorMessageResponse(e.getMessage()), HttpStatus.NOT_FOUND);
    } catch (InvalidOperation e) {
      return new ResponseEntity<>(new ErrorMessageResponse(e.getMessage()), HttpStatus.FORBIDDEN);
    }
  }

  @Operation(summary = "invite someone to group channel")
  @AuthenticatedApi
  @SecurityRequirement(name = "jwt")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "403",
            description =
                "cannot invite target user into the group channel, you may not in the channel or"
                    + " target user already in the channel",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorMessageResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "the channel or target user does not exist",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorMessageResponse.class))),
        @ApiResponse(
            responseCode = "200",
            description = "invite target user into the group channel and wait for accept",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = GroupMessageDto.class))),
      })
  @RequestMapping(path = "/invite", method = RequestMethod.POST)
  public ResponseEntity<Object> invite(@Valid @RequestBody ChannelUserRequest request) {
    try {
      GroupMessageDto message =
          channelService.inviteToChannel(
              AuthUtil.currentUserDetail().getId(),
              request.getTargetUserId(),
              request.getChannelId());
      messageService.deliverMessage(message);
      return ResponseEntity.ok(message);
    } catch (InvalidOperation e) {
      return new ResponseEntity<>(new ErrorMessageResponse(e.getMessage()), HttpStatus.FORBIDDEN);
    } catch (ChannelDoesNotExist | UserDoesNotExist e) {
      return new ResponseEntity<>(new ErrorMessageResponse(e.getMessage()), HttpStatus.NOT_FOUND);
    }
  }

  @Operation(summary = "accept invitation to group channel")
  @AuthenticatedApi
  @SecurityRequirement(name = "jwt")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "403",
            description = "cannot join the group channel",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorMessageResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "the channel does not exist",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorMessageResponse.class))),
        @ApiResponse(
            responseCode = "200",
            description = "join the group channel",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = GroupMessageDto.class))),
      })
  @RequestMapping(path = "/accept", method = RequestMethod.POST)
  public ResponseEntity<Object> accept(@Valid @RequestBody ChannelRequest request)
      throws UserDoesNotExist {
    try {
      GroupMessageDto message =
          channelService.acceptInvitationOfChannel(
              AuthUtil.currentUserDetail().getId(), request.getChannelId());
      messageService.deliverMessage(message);
      return ResponseEntity.ok(message);
    } catch (InvalidOperation e) {
      return new ResponseEntity<>(new ErrorMessageResponse(e.getMessage()), HttpStatus.FORBIDDEN);
    } catch (ChannelDoesNotExist e) {
      return new ResponseEntity<>(new ErrorMessageResponse(e.getMessage()), HttpStatus.NOT_FOUND);
    }
  }

  @Operation(summary = "get invited group channels id")
  @AuthenticatedApi
  @SecurityRequirement(name = "jwt")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "ids of invited channels",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = SliceList.class))),
      })
  @RequestMapping(path = "/invitation", method = RequestMethod.GET)
  public ResponseEntity<Object> getInvitations(@ParameterObject @Valid PageRequestWithSince request)
      throws UserDoesNotExist {
    SliceList<String> sliceList =
        channelService.getInvitedChannels(
            AuthUtil.currentUserDetail().getId(), request.getSince(), request.getPageRequest());
    return ResponseEntity.ok(sliceList);
  }

  @Operation(summary = "kick off someone in group channel")
  @AuthenticatedApi
  @SecurityRequirement(name = "jwt")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "403",
            description = "ypu or target user is not in the channel",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorMessageResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "the channel or target user does not exist",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorMessageResponse.class))),
        @ApiResponse(
            responseCode = "200",
            description = "kick off target user from group channel",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = GroupMessageDto.class))),
      })
  @RequestMapping(path = "/kickOff", method = RequestMethod.POST)
  public ResponseEntity<Object> kickOff(@Valid @RequestBody ChannelUserRequest request) {
    try {
      GroupMessageDto message =
          channelService.removeFromChannel(
              AuthUtil.currentUserDetail().getId(),
              request.getTargetUserId(),
              request.getChannelId());
      messageService.deliverMessage(message);
      return ResponseEntity.ok(message);
    } catch (InvalidOperation e) {
      return new ResponseEntity<>(new ErrorMessageResponse(e.getMessage()), HttpStatus.FORBIDDEN);
    } catch (ChannelDoesNotExist | UserDoesNotExist e) {
      return new ResponseEntity<>(new ErrorMessageResponse(e.getMessage()), HttpStatus.NOT_FOUND);
    }
  }

  @Operation(summary = "leave group channel")
  @AuthenticatedApi
  @SecurityRequirement(name = "jwt")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "403",
            description = "ypu are not in the channel",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorMessageResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "the channel does not exist",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorMessageResponse.class))),
        @ApiResponse(
            responseCode = "200",
            description = "leave the group channel",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = GroupMessageDto.class))),
      })
  @RequestMapping(path = "/leave", method = RequestMethod.POST)
  public ResponseEntity<Object> leave(@Valid @RequestBody ChannelRequest request)
      throws UserDoesNotExist {
    try {
      GroupMessageDto message =
          channelService.leaveChannel(AuthUtil.currentUserDetail().getId(), request.getChannelId());
      messageService.deliverMessage(message);
      return ResponseEntity.ok(message);
    } catch (InvalidOperation e) {
      return new ResponseEntity<>(new ErrorMessageResponse(e.getMessage()), HttpStatus.FORBIDDEN);
    } catch (ChannelDoesNotExist e) {
      return new ResponseEntity<>(new ErrorMessageResponse(e.getMessage()), HttpStatus.NOT_FOUND);
    }
  }

  @Operation(summary = "get profiles of all group channels of current user")
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
                    schema = @Schema(implementation = SliceOfGroupChannel.class))),
      })
  @RequestMapping(path = "/list", method = RequestMethod.GET)
  public ResponseEntity<Object> list(@ParameterObject @Valid PageRequestWithSince request)
      throws UserDoesNotExist {
    SliceList<GroupChannelProfile> sliceList =
        channelService.getAllChannels(
            AuthUtil.currentUserDetail().getId(), request.getSince(), request.getPageRequest());
    return ResponseEntity.ok(new SliceOfGroupChannel(sliceList));
  }

  @Operation(summary = "get profile of group channel")
  @AuthenticatedApi
  @SecurityRequirement(name = "jwt")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "403",
            description = "you are not the member in target channel",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorMessageResponse.class))),
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
                    schema = @Schema(implementation = GroupChannelProfile.class))),
      })
  @RequestMapping(path = "/profile", method = RequestMethod.GET)
  public ResponseEntity<Object> profile(@ParameterObject @Valid ChannelRequest request)
      throws UserDoesNotExist {
    try {
      GroupChannelProfile profile =
          channelService.getChannelProfile(
              AuthUtil.currentUserDetail().getId(), request.getChannelId());
      return ResponseEntity.ok(profile);
    } catch (InvalidOperation e) {
      return new ResponseEntity<>(new ErrorMessageResponse(e.getMessage()), HttpStatus.FORBIDDEN);
    } catch (ChannelDoesNotExist e) {
      return new ResponseEntity<>(new ErrorMessageResponse(e.getMessage()), HttpStatus.NOT_FOUND);
    }
  }

  @Operation(summary = "subscribe to all group channels of current user")
  @AuthenticatedApi
  @SecurityRequirement(name = "jwt-in-query")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description =
                "subscribe to all group channels via SSE, "
                    + "you will receive messages in data event like below",
            content =
                @Content(
                    mediaType = "application/json",
                    array =
                        @ArraySchema(schema = @Schema(implementation = GroupMessageDto.class)))),
      })
  @RequestMapping(path = "/subscribe", method = RequestMethod.GET)
  public Object subscribe(@ParameterObject @Valid SubscribeRequest request)
      throws UserDoesNotExist {
    return channelService.subscribe(AuthUtil.currentUserDetail().getId());
  }
}
