package com.joejoe2.chat.models;

import com.joejoe2.chat.exception.InvalidOperation;
import java.time.Instant;
import java.util.*;
import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

@Data
@NoArgsConstructor
@Entity
@BatchSize(size = 128)
@Table(name = "group_channel", indexes = @Index(columnList = "updateAt DESC"))
public class GroupChannel extends TimeStampBase {
  @Version
  @Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT now()")
  private Instant version;

  @Column(nullable = false, length = 128)
  String name = "";

  @ManyToMany
  @BatchSize(size = 128) // for each PrivateChannels->getMembers
  @JoinTable(
      name = "group_channels_users",
      joinColumns = {@JoinColumn(name = "group_channel_id", nullable = false)},
      inverseJoinColumns = {@JoinColumn(name = "user_id", nullable = false)})
  Set<User> members = new HashSet<>();

  @OneToMany(mappedBy = "channel", cascade = CascadeType.ALL)
  @BatchSize(size = 32) // for each channels->getInvitations
  Set<GroupInvitation> invitations = new HashSet<>();

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "channel", orphanRemoval = true)
  List<GroupMessage> messages = new ArrayList<>();

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn
  GroupMessage lastMessage;

  public GroupChannel(Set<User> members) {
    this.members = members;
  }

  public void invite(User inviter, User invitee) throws InvalidOperation {
    if (!members.contains(inviter))
      throw new InvalidOperation("inviter is not in members of the channel !");
    GroupInvitation invitation = new GroupInvitation(invitee, this);
    if (members.contains(invitee) || invitations.contains(invitation))
      throw new InvalidOperation("invitee is in the channel !");
    checkNumOfMembers();

    GroupMessage invitationMessage = GroupMessage.inviteMessage(this, inviter, invitee);
    messages.add(invitationMessage);
    invitations.add(new GroupInvitation(invitee, this, invitationMessage));
    lastMessage = invitationMessage;
  }

  public void kickOff(User actor, User target) throws InvalidOperation {
    if (actor.equals(target)) throw new InvalidOperation("actor cannot kick off itself !");
    if (!members.contains(actor))
      throw new InvalidOperation("actor is not in members of the channel !");
    if (!members.contains(target))
      throw new InvalidOperation("target is not in members of the channel !");

    members.remove(target);
    GroupMessage leaveMessage = GroupMessage.leaveMessage(this, actor, target);
    messages.add(leaveMessage);
    lastMessage = leaveMessage;
  }

  public void leave(User user) throws InvalidOperation {
    if (!members.contains(user))
      throw new InvalidOperation("user is not in members of the channel !");
    // todo: handle channel with 0 members ?
    if (members.size() == 1) throw new InvalidOperation("user is the last member of the channel !");

    members.remove(user);
    GroupMessage leaveMessage = GroupMessage.leaveMessage(this, user, user);
    messages.add(leaveMessage);
    lastMessage = leaveMessage;
  }

  public void acceptInvitation(User invitee) throws InvalidOperation {
    GroupInvitation invitation = new GroupInvitation(invitee, this);
    if (!invitations.contains(invitation)) throw new InvalidOperation("no invitation !");

    invitations.remove(invitation);
    members.add(invitee);
    checkNumOfMembers();

    GroupMessage joinMessage = GroupMessage.joinMessage(this, invitee);
    messages.add(joinMessage);
    lastMessage = joinMessage;
  }

  public void addMessage(User from, String message) throws InvalidOperation {
    if (!members.contains(from))
      throw new InvalidOperation("user is not in members of the channel !");
    GroupMessage groupMessage = new GroupMessage(this, from, message);
    messages.add(groupMessage);
    lastMessage = groupMessage;
  }

  void checkNumOfMembers() throws InvalidOperation {
    if (members.size() == 0)
      throw new InvalidOperation("GroupChannel must contain at least 1 members !");
    else if (members.size() > 1024)
      throw new InvalidOperation("GroupChannel must contain at most 1024 members !");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof GroupChannel)) return false;
    GroupChannel that = (GroupChannel) o;
    return Objects.equals(getId(), that.getId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "GroupChannel{"
        + "members="
        + members
        + ", createAt="
        + createAt
        + ", updateAt="
        + updateAt
        + ", id="
        + id
        + '}';
  }
}