package com.joejoe2.chat.models;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Data
@NoArgsConstructor
@Entity
@BatchSize(size = 128)
@Table(name = "private_channel",
        indexes = {@Index(columnList = "uniqueUserIds")})
public class PrivateChannel extends TimeStampBase{
    public final static String WITH_MEMBERS = "PrivateChannelGraph.with_members";

    @Version
    @Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT now()")
    private Instant version;

    @ManyToMany
    @BatchSize(size = 128) // for each PrivateChannels->getMembers
    @JoinTable(name = "private_channels_users",
            joinColumns = {@JoinColumn(name = "private_channel_id", nullable = false)},
            inverseJoinColumns = {@JoinColumn(name = "user_id", nullable = false)})
    Set<User> members;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "channel")
    List<PrivateMessage> messages;

    @OneToOne
    @JoinColumn
    PrivateMessage lastMessage;

    public PrivateChannel(Set<User> members) {
        this.members = members;
    }

    //concat two user ids in sorted to prevent duplicate channel between them
    @Column(unique = true, nullable = false, updatable = false)
    String uniqueUserIds;

    @PrePersist
    void calculateUniqueByUserIds(){
        List<String> ids = members.stream().sorted(Comparator.comparing(User::getId))
                .map(user -> user.getId().toString()).toList();
        StringBuilder uniqueIds = new StringBuilder();
        for (String id:ids)
            uniqueIds.append(id);
        this.uniqueUserIds = uniqueIds.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PrivateChannel)) return false;
        PrivateChannel channel = (PrivateChannel) o;
        return Objects.equals(id, channel.id) && Objects.equals(uniqueUserIds, channel.uniqueUserIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, uniqueUserIds);
    }

    @Override
    public String toString() {
        return "PrivateChannel{" +
                "id=" + id +
                ", members=" + members +
                ", lastMessage=" + lastMessage +
                ", createAt=" + createAt +
                ", updateAt=" + updateAt +
                '}';
    }
}
