package com.ethk3.novaonepro;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** In-memory, one-boot user authority. Revocation never waits for hashing/SAF.
 * CAS(PENDING,COMMITTING) is the linearization point immediately before unlink.
 * A crash drops every ticket; durable restore files cannot recreate one. */
final class LegacyRetirementAuthorizationR335 {
    static final long VALID_MS=60*60*1000L;
    interface Clock {long now();}
    private final Clock clock;private final ConcurrentHashMap<String,Ticket> tickets=new ConcurrentHashMap<>();
    static final class Ticket {
        final String caseId,fingerprint,token;final long issued;final AtomicInteger state=new AtomicInteger();
        Ticket(String c,String f,String t,long at){caseId=c;fingerprint=f;token=t;issued=at;}
    }
    LegacyRetirementAuthorizationR335(){this(()->System.nanoTime()/1000000L);}
    LegacyRetirementAuthorizationR335(Clock clock){this.clock=clock;}
    long now(){return clock.now();}
    String confirm(String caseId,String fingerprint,long reviewedAt)throws IOException {
        long now=now();if(now<reviewedAt||now-reviewedAt>VALID_MS||!caseId.matches("[0-9a-f]{64}")||!fingerprint.matches("[0-9a-f]{64}"))throw new IOException("R335_CUSTODY_REVIEW_EXPIRED_OR_IDENTITY");
        revoke(caseId);for(Ticket old:tickets.values())if(old.state.get()!=0||now<old.issued||now-old.issued>VALID_MS)tickets.remove(old.token,old);
        if(tickets.size()>=64)throw new IOException("R335_CUSTODY_AUTHORIZATION_CAPACITY");String token=UUID.randomUUID().toString();tickets.put(token,new Ticket(caseId,fingerprint,token,now));return token;
    }
    void revoke(String id){for(Ticket ticket:tickets.values())if(id==null||id.equals(ticket.caseId)){if(ticket.state.compareAndSet(0,1))tickets.remove(ticket.token,ticket);}}
    Ticket check(String token,String caseId,String fingerprint)throws IOException {
        Ticket t=token==null?null:tickets.get(token);long now=now();if(t==null||t.state.get()!=0||!t.caseId.equals(caseId)||!t.fingerprint.equals(fingerprint)||now<t.issued||now-t.issued>VALID_MS)throw new IOException("R335_CUSTODY_AUTHORIZATION_REVOKED_EXPIRED_OR_CHANGED");return t;
    }
    void beginCommit(String token,String caseId,String fingerprint)throws IOException {
        Ticket t=check(token,caseId,fingerprint);if(!t.state.compareAndSet(0,2))throw new IOException("R335_CUSTODY_AUTHORIZATION_REVOKED_AT_COMMIT");
    }
    void finish(String token){Ticket t=token==null?null:tickets.remove(token);if(t!=null)t.state.set(3);}
}
