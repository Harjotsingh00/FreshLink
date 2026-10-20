package freshlink;

import java.time.LocalDateTime;
import java.util.UUID;

/** A locally stored in-app workflow notification. */
public record AppNotification(String id,String recipient,String message,LocalDateTime createdAt,boolean read) {
    public AppNotification(String recipient,String message){this(UUID.randomUUID().toString(),recipient,message,LocalDateTime.now(),false);}
    public AppNotification markRead(){return new AppNotification(id,recipient,message,createdAt,true);}
}
