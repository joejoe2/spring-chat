package com.joejoe2.chat.data;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageRequest {
    @Parameter(description = "must >= 0")
    @Min(value = 0, message = "page must >= 0 !")
    @NotNull(message = "page is missing !")
    private Integer page;

    @Parameter(description = "must >= 1")
    @Min(value = 1, message = "page must >= 1 !")
    @NotNull(message = "size is missing !")
    private Integer size;
}
