package com.joejoe2.chat.controller;

import com.joejoe2.chat.controller.constraint.auth.AuthenticatedApi;
import com.joejoe2.chat.data.ErrorMessageResponse;
import com.joejoe2.chat.data.PageRequest;
import com.joejoe2.chat.data.channel.PageOfChannel;
import com.joejoe2.chat.data.channel.profile.PublicChannelProfile;
import com.joejoe2.chat.data.message.PublicMessageDto;
import com.joejoe2.chat.data.message.request.GetPrivateMessageSinceRequest;
import com.joejoe2.chat.data.message.PrivateMessageDto;
import com.joejoe2.chat.data.SliceList;
import com.joejoe2.chat.data.channel.SliceOfChannel;
import com.joejoe2.chat.data.message.request.PublishPrivateMessageRequest;
import com.joejoe2.chat.data.message.SliceOfMessage;
import com.joejoe2.chat.data.channel.profile.PrivateChannelProfile;
import com.joejoe2.chat.data.channel.request.*;
import com.joejoe2.chat.exception.AlreadyExist;
import com.joejoe2.chat.exception.ChannelDoesNotExist;
import com.joejoe2.chat.exception.InvalidOperation;
import com.joejoe2.chat.exception.UserDoesNotExist;
import com.joejoe2.chat.service.channel.PrivateChannelService;
import com.joejoe2.chat.service.message.PrivateMessageService;
import com.joejoe2.chat.utils.AuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.validation.Valid;

@Controller
@RequestMapping(path = "/api/channel/private")
public class PrivateChannelController {
    @Autowired
    PrivateChannelService channelService;
    @Autowired
    PrivateMessageService messageService;

    @Operation(summary = "publish message to private channel")
    @AuthenticatedApi
    @SecurityRequirement(name = "jwt")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "403", description = "current user is not a member" +
                    " of target channel",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorMessageResponse.class))),
            @ApiResponse(responseCode = "404", description = "target channel is not exist",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorMessageResponse.class))),
            @ApiResponse(
                    responseCode = "200", description = "publish message to target channel",
                    content = @Content),
    })
    @RequestMapping(path = "/publishMessage", method = RequestMethod.POST)
    public ResponseEntity<Object> publishMessage(@RequestBody @Valid PublishPrivateMessageRequest request) throws UserDoesNotExist {
        try {
            PrivateMessageDto message = messageService.createMessage(AuthUtil.currentUserDetail().getId(), request.getChannelId(), request.getMessage());
            messageService.deliverMessage(message);
        }catch (ChannelDoesNotExist e){
            return new ResponseEntity<>(new ErrorMessageResponse(e.getMessage()), HttpStatus.NOT_FOUND);
        } catch (InvalidOperation e) {
            return new ResponseEntity<>(new ErrorMessageResponse(e.getMessage()), HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "get all messages in private channels of current user")
    @AuthenticatedApi
    @SecurityRequirement(name = "jwt")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", description = "messages",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SliceOfMessage.class))),
    })
    @RequestMapping(path = "/getAllMessages", method = RequestMethod.GET)
    public ResponseEntity<Object> getMessages(@ParameterObject @Valid PageRequest request) throws UserDoesNotExist {
        SliceList<PrivateMessageDto> sliceList = messageService.getAllMessages(AuthUtil.currentUserDetail().getId(), request);
        return ResponseEntity.ok(new SliceOfMessage<>(sliceList));
    }

    @Operation(summary = "get all messages in private channels of current user since")
    @AuthenticatedApi
    @SecurityRequirement(name = "jwt")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", description = "messages",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SliceOfMessage.class))),
    })
    @RequestMapping(path = "/getMessagesSince", method = RequestMethod.GET)
    public ResponseEntity<Object> getMessagesSince(@ParameterObject @Valid GetPrivateMessageSinceRequest request) throws UserDoesNotExist {
        SliceList<PrivateMessageDto> sliceList = messageService.
                getAllMessages(AuthUtil.currentUserDetail().getId(), request.getSince(), request.getPageRequest());
        return ResponseEntity.ok(new SliceOfMessage<>(sliceList));
    }

    @Operation(summary = "create public channel")
    @AuthenticatedApi
    @SecurityRequirement(name = "jwt")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "403", description = "the channel between you and target user" +
                    " is already exist, target user == current user, or target user is not exist",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorMessageResponse.class))),
            @ApiResponse(
                    responseCode = "200", description = "create private channel between " +
                    "current user and target user",
                    content = @Content),
    })
    @RequestMapping(path = "/create", method = RequestMethod.POST)
    public ResponseEntity<Object> create(@Valid @RequestBody CreatePrivateChannelRequest request){
        try {
            PrivateChannelProfile channel = channelService.
                    createChannelBetween(AuthUtil.currentUserDetail().getId(), request.getTargetUserId());
            return new ResponseEntity<>(channel, HttpStatus.OK);
        } catch (AlreadyExist | InvalidOperation | UserDoesNotExist e) {
            return new ResponseEntity<>(new ErrorMessageResponse(e.getMessage()), HttpStatus.FORBIDDEN);
        }
    }

    @Operation(summary = "get profiles of all private channels of current user")
    @AuthenticatedApi
    @SecurityRequirement(name = "jwt")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", description = "profiles of channel",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SliceOfChannel.class))),
    })
    @RequestMapping(path = "/list", method = RequestMethod.GET)
    public ResponseEntity<Object> list(@ParameterObject @Valid PageRequest request) throws UserDoesNotExist {
        SliceList<PrivateChannelProfile> sliceList = channelService.getAllChannels(AuthUtil.currentUserDetail().getId(), request);
        return ResponseEntity.ok(new SliceOfChannel(sliceList));
    }

    @Operation(summary = "get profile of private channel")
    @AuthenticatedApi
    @SecurityRequirement(name = "jwt")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "403", description = "you are not the member in target channel",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorMessageResponse.class))),
            @ApiResponse(responseCode = "404", description = "target channel is not exist",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorMessageResponse.class))),
            @ApiResponse(
                    responseCode = "200", description = "profile of channel",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PrivateChannelProfile.class))),
    })
    @RequestMapping(path = "/profile", method = RequestMethod.GET)
    public ResponseEntity<Object> profile(@ParameterObject @Valid GetChannelProfileRequest request) throws UserDoesNotExist {
        try {
            PrivateChannelProfile profile = channelService.getChannelProfile(AuthUtil.currentUserDetail().getId(), request.getChannelId());
            return ResponseEntity.ok(profile);
        } catch (InvalidOperation e) {
            return new ResponseEntity<>(new ErrorMessageResponse(e.getMessage()), HttpStatus.FORBIDDEN);
        } catch (ChannelDoesNotExist e) {
            return new ResponseEntity<>(new ErrorMessageResponse(e.getMessage()), HttpStatus.NOT_FOUND);
        }
    }

    @Operation(summary = "subscribe to all private channels of current user")
    @AuthenticatedApi
    @SecurityRequirement(name = "jwt-in-query")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", description = "subscribe to all private channels via SSE, " +
                    "you will receive messages in data event like below",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = PrivateMessageDto.class)))),
    })
    @RequestMapping(path = "/subscribe", method = RequestMethod.GET)
    public Object subscribe(@ParameterObject @Valid SubscribePrivateChannelRequest request) throws UserDoesNotExist {
        return channelService.subscribe(AuthUtil.currentUserDetail().getId());
    }
}
