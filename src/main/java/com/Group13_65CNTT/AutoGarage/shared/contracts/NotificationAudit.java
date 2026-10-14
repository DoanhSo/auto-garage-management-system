package com.Group13_65CNTT.AutoGarage.shared.contracts;

/** I08 (TV1): outbox and audit writes join the business transaction; send after commit. */
public interface NotificationAudit {
    enum Channel { WEB, EMAIL, SMS }
    record Notice(Long userId, Long customerId, String recipientKey, String destination,
                  String eventKey, Channel channel, String payloadJson) {}
    void enqueue(Notice notice);
    /** Reusing a key with a different hash must fail; an identical request returns its stored result. */
    String findCompletedRequest(String requestKey, String requestHash);
    void recordCompletedRequest(long actorUserId, String action, String entityType, long entityId,
                                String requestKey, String requestHash, String resultJson);
}
