package model;

import java.time.Instant;

public class Request {

    private final String id;
    private final String userId;
    private final String type;
    private final Instant timestamp;

    public Request(String id, String userId, String type) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.timestamp = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getType() {
        return type;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "Request{"
                + "id='" + id + '\''
                + ", userId='" + userId + '\''
                + ", type='" + type + '\''
                + ", timestamp=" + timestamp
                + '}';
    }
}