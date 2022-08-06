package com.joejoe2.chat.data.message.request;

import com.joejoe2.chat.data.PageRequest;
import com.joejoe2.chat.validation.constraint.UUID;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDateTime;

@Data
public class GetPublicMessageSinceRequest {
    @Parameter(description = "id of target channel")
    @UUID(message = "invalid channel id !")
    private String channelId;

    @Parameter(description = "page parameters")
    @Valid
    @NotNull(message = "page request is missing !")
    private PageRequest pageRequest;

    @Parameter(description = "since in UTC")
    @NotNull(message = "invalid since format !")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    Instant since;
}
