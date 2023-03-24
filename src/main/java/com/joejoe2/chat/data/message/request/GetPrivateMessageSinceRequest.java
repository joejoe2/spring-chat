package com.joejoe2.chat.data.message.request;

import com.joejoe2.chat.data.PageRequest;
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
