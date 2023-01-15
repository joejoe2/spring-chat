package com.joejoe2.chat.data.message.request;

import com.joejoe2.chat.data.PageRequest;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.Instant;

@Data
public class GetPrivateMessageSinceRequest {
    @Parameter(description = "since in UTC")
    @NotNull(message = "invalid since format !")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    Instant since;

    @Parameter(description = "page parameters")
    @Valid
    @NotNull(message = "page request is missing !")
    private PageRequest pageRequest;
}
