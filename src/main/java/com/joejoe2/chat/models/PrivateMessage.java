package com.joejoe2.chat.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "private_message",
        indexes = {@Index(columnList = "to_id"),
                @Index(columnList = "from_id"),
                @Index(columnList = "channel_id")})
public class PrivateMessage extends TimeStampBase{
    @Version
    @Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT now()")
    private Instant version;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private PrivateChannel channel;

    @Column(length = 32, nullable = false)
    @Enumerated(EnumType.STRING)
    private MessageType messageType=MessageType.MESSAGE; //code level default

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    User from;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    User to;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    public PrivateMessage(PrivateChannel channel, User from, User to, String content) {
        if (from.equals(to)) throw new IllegalArgumentException("from cannot be same with to !");
        this.channel = channel;
        this.from = from;
        this.to = to;
        this.content = content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PrivateMessage)) return false;
        PrivateMessage that = (PrivateMessage) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PrivateMessage{" +
                "id=" + id +
                ", channel=" + channel +
                ", messageType=" + messageType +
                ", from=" + from +
                ", to=" + to +
                ", content='" + content + '\'' +
                ", createAt=" + createAt +
                ", updateAt=" + updateAt +
                '}';
    }
}
