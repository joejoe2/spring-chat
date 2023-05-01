package com.joejoe2.chat.data.channel.request;

import com.joejoe2.chat.data.PageRequest;
import com.joejoe2.chat.validation.constraint.UUID;
import io.swagger.v3.oas.annotations.Parameter;
import java.time.Instant;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelPageRequestWithSince {
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
