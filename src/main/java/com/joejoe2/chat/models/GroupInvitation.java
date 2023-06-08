package com.joejoe2.chat.models;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "group_channels_pending_users", indexes = @Index(columnList = "createAt DESC"))
@Data
@NoArgsConstructor
public class GroupInvitation {
    @EmbeddedId
    GroupInvitationKey key;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @MapsId("channelId")
    @JoinColumn(name = "group_channel_id", nullable = false)
    GroupChannel channel;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE) // if delete invitationMessage => also delete this
    @JoinColumn
    GroupMessage invitationMessage;

    @CreationTimestamp
    Instant createAt;

    public GroupInvitation(User user, GroupChannel channel, GroupMessage invitationMessage) {
        this.user = user;
        this.channel = channel;
        this.key = new GroupInvitationKey(user.getId(), channel.getId());
        if (!invitationMessage.getMessageType().equals(MessageType.INVITATION))
            throw new IllegalArgumentException("message type must be INVITATION !");
        this.invitationMessage = invitationMessage;
    }

    public GroupInvitation(User user, GroupChannel channel) {
        this.user = user;
        this.channel = channel;
        this.key = new GroupInvitationKey(user.getId(), channel.getId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GroupInvitation that)) return false;
        return key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }
}
