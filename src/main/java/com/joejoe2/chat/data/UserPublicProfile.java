package com.joejoe2.chat.data;

import com.joejoe2.chat.models.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
@NoArgsConstructor
@AllArgsConstructor
@Data
public class UserPublicProfile {
    @Schema(description = "user id")
    String id;
    @Schema(description = "username")
    String username;

    public UserPublicProfile(User user){
        this.id = user.getId().toString();
        this.username = user.getUserName();
    }
}
