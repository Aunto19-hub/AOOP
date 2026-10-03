package com.seamline.repository;

import com.seamline.domain.ChatMessage;
import com.seamline.domain.Employee;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByRecipientIsNullAndCreatedAtAfterOrderByCreatedAtAsc(Instant since);

    @Query("select m from ChatMessage m where m.recipient is null order by m.createdAt desc")
    List<ChatMessage> findRecentGroup(Pageable pageable);

    @Query("select m from ChatMessage m where m.recipient is not null "
            + "and ((m.sender = :a and m.recipient = :b) or (m.sender = :b and m.recipient = :a)) "
            + "and m.createdAt > :since order by m.createdAt asc")
    List<ChatMessage> findThreadSince(@Param("a") Employee a, @Param("b") Employee b, @Param("since") Instant since);

    @Query("select m from ChatMessage m where m.recipient is not null "
            + "and ((m.sender = :a and m.recipient = :b) or (m.sender = :b and m.recipient = :a)) "
            + "order by m.createdAt desc")
    List<ChatMessage> findRecentThread(@Param("a") Employee a, @Param("b") Employee b, Pageable pageable);

    /** Marks every unread message {@code reader} has received from {@code sender} as read. */
    @Modifying
    @Query("update ChatMessage m set m.readAt = :at "
            + "where m.recipient = :reader and m.sender = :sender and m.readAt is null")
    void markThreadRead(@Param("reader") Employee reader, @Param("sender") Employee sender, @Param("at") Instant at);

    /** The latest time {@code recipient} read a message {@code sender} sent them, or null if none has been read. */
    @Query("select max(m.readAt) from ChatMessage m where m.sender = :sender and m.recipient = :recipient")
    Instant findLastReadAt(@Param("sender") Employee sender, @Param("recipient") Employee recipient);

    /** Whether {@code recipient} has any unread message from {@code sender} — drives the contact list's unread dot. */
    boolean existsBySenderAndRecipientAndReadAtIsNull(Employee sender, Employee recipient);
}
