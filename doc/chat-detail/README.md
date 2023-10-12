# Technical details of the chat

## Chat in public channel

- A public channel allows any user to chat online.
- Each subscription target is channel id.
- Query history messages by channel id.

![image](public-chat.png)

## Chat in private channel

- Every two users can chat with each other in a private channel.
- Each subscription target is user id.
- Query history messages by user id 

![image](private-chat.png)

## Chat in group channel

- A group channel allows any members to chat online. A user should 
be invited to the channel first by any member, and the user should 
accept the invitation to become a member. 
- Each subscription target is user id.
- Query history messages by channel id.

![image](group-chat.png)

## Block a private channel

If userA block userB, 

- any future msgs from userB to userA is invisible, unblocked will not take these msgs back
- publish msg endpoint will return 403 when userB sends msg to userA (actually do not persist/deliver msgs)
- userB can still get new msgs from userA until userB block userA
