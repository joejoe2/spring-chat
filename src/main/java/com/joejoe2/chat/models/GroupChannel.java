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
  private String name = "";

  @ManyToMany
  @BatchSize(size = 128)
  @JoinTable(
      name = "group_channels_users",
      joinColumns = {@JoinColumn(name = "group_channel_id", nullable = false)},
      inverseJoinColumns = {@JoinColumn(name = "user_id", nullable = false)})
  private Set<User> members = new HashSet<>();

  @ManyToMany
  @BatchSize(size = 128)
  @JoinTable(
      name = "group_channels_administrators",
      joinColumns = {@JoinColumn(name = "group_channel_id", nullable = false)},
      inverseJoinColumns = {@JoinColumn(name = "user_id", nullable = false)})
  private Set<User> administrators = new HashSet<>();

  @ManyToMany
  @BatchSize(size = 128)
  @JoinTable(
      name = "group_channels_banned_users",
      joinColumns = {@JoinColumn(name = "group_channel_id", nullable = false)},
      inverseJoinColumns = {@JoinColumn(name = "user_id", nullable = false)})
  private Set<User> banned = new HashSet<>();

  @OneToMany(mappedBy = "channel", cascade = CascadeType.ALL)
  @BatchSize(size = 32)
  private Set<GroupInvitation> invitations = new HashSet<>();

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "channel", orphanRemoval = true)
  private List<GroupMessage> messages = new ArrayList<>();

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn
  private GroupMessage lastMessage;

  public GroupChannel(User creator) {
    this.members.add(creator);
    this.administrators.add(creator);
  }

  /**
   * check the user have admin permissions for actions, note this will always return true if there
   * is no administrator due to the old version of the group channel
   *
   * @return true if administrators contains user or there is no administrator
   */
  private boolean isAdmin(User user) {
    return administrators.contains(user) || administrators.size() == 0;
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

  public void kickOff(User admin, User target) throws InvalidOperation {
    if (admin.equals(target)) throw new InvalidOperation("cannot kick off itself !");
    if (!isAdmin(admin))
      throw new InvalidOperation("admin is not an valid administrator in the channel !");
    if (administrators.contains(target))
      throw new InvalidOperation("cannot kick off target because it is an administrator !");
    if (!members.contains(target))
      throw new InvalidOperation("target user is not in members of the channel !");

    members.remove(target);
    banned.remove(target);
    GroupMessage leaveMessage = GroupMessage.leaveMessage(this, admin, target);
    messages.add(leaveMessage);
    lastMessage = leaveMessage;
  }

  public void leave(User user) throws InvalidOperation {
    if (!members.contains(user))
      throw new InvalidOperation("user is not in members of the channel !");
    if (administrators.contains(user) && administrators.size() == 1)
      throw new InvalidOperation("user is the last administrator of the channel !");
    // todo: handle channel with 0 members ?
    if (members.size() == 1) throw new InvalidOperation("user is the last member of the channel !");

    members.remove(user);
    administrators.remove(user);
    banned.remove(user);
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

  /** let admin editBanned target user, cannot {@link #addMessage} until unbanned */
  public void ban(User admin, User target) throws InvalidOperation {
    if (admin.equals(target)) throw new InvalidOperation("cannot editBanned itself !");
    if (!isAdmin(admin))
      throw new InvalidOperation("admin is not an valid administrator in the channel !");
    if (administrators.contains(target))
      throw new InvalidOperation("cannot editBanned target because it is an administrator !");
    if (!members.contains(target))
      throw new InvalidOperation("target user is not in members of the channel !");

    banned.add(target);
    GroupMessage banMessage = GroupMessage.banMessage(this, admin, target);
    messages.add(banMessage);
    lastMessage = banMessage;
  }

  /** let admin unban target user */
  public void unban(User admin, User target) throws InvalidOperation {
    if (admin.equals(target)) throw new InvalidOperation("cannot unban itself !");
    if (!isAdmin(admin))
      throw new InvalidOperation("admin is not an valid administrator in the channel !");
    if (!members.contains(target))
      throw new InvalidOperation("target user is not in members of the channel !");
    if (!banned.contains(target)) throw new InvalidOperation("target user has not been banned !");

    banned.remove(target);
    GroupMessage unbanMessage = GroupMessage.unbanMessage(this, admin, target);
    messages.add(unbanMessage);
    lastMessage = unbanMessage;
  }

  /** let admin add target user to administrators, no op if target user is an administrator */
  public void addToAdministrators(User admin, User target) throws InvalidOperation {
    if (admin.equals(target)) throw new InvalidOperation("cannot add itself !");
    if (!administrators.contains(admin))
      throw new InvalidOperation("admin is not an valid administrator in the channel !");
    if (!members.contains(target))
      throw new InvalidOperation("target user is not in members of the channel !");
    if (banned.contains(target)) throw new InvalidOperation("target user has been banned !");

    administrators.add(target);
  }

  /**
   * let admin remove target user from administrators, no op if target user is not an administrator
   */
  public void removeFromAdministrators(User admin, User target) throws InvalidOperation {
    if (admin.equals(target)) throw new InvalidOperation("cannot remove itself !");
    if (!administrators.contains(admin))
      throw new InvalidOperation("admin is not an valid administrator in the channel !");

    administrators.remove(target);
  }

  public void addMessage(User from, String message) throws InvalidOperation {
    if (!members.contains(from))
      throw new InvalidOperation("user is not in members of the channel !");
    if (banned.contains(from)) throw new InvalidOperation("user has been banned !");
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
