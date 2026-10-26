package com.ethk3.novaonepro.strategyv2;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic reference for a prospectively registered paired comparison.
 * No Android, database, certificate, REAL permission, or wall-clock owner is provided here.
 * Production must persist each event/result atomically and reconstruct this object on restart.
 * The lower bound concerns the average conditional child-minus-parent hit probability over
 * declared opportunities. Missing pairs receive -1; complete-case means are descriptive only.
 */
public final class PairedEvidence {
    public enum Scope { SAME_POSITION_K3, POLICY_K3 }

    public static final class Identity {
        public final String candidateId, policyHash, structureHash, runHash;
        public Identity(String candidateId, String policyHash, String structureHash, String runHash) {
            this.candidateId = text(candidateId, "candidateId");
            this.policyHash = sha(policyHash); this.structureHash = sha(structureHash);
            this.runHash = sha(runHash);
        }
        private String wire() { return digest(candidateId, policyHash, structureHash, runHash); }
    }

    public static final class Protocol {
        public final String claimId, familyRootHash, sourceEpoch, hash;
        public final Identity parent, child;
        public final Scope scope;
        public final long registeredAt;
        public final int globalClaimOrdinal, blockSize;
        public final double familyAlpha, minimumCoverage, minimumPracticalGain;
        private final int[] lookSchedule;
        public Protocol(String claimId, String familyRootHash, String sourceEpoch, Identity parent, Identity child,
                Scope scope, long registeredAt, double familyAlpha, int globalClaimOrdinal,
                int[] lookSchedule, double minimumCoverage, double minimumPracticalGain,
                int blockSize) {
            this.claimId = sha(claimId); this.familyRootHash=sha(familyRootHash); this.sourceEpoch = sha(sourceEpoch);
            if (parent == null || child == null || scope == null || parent.candidateId.equals(child.candidateId))
                throw new IllegalArgumentException("PAIR_IDENTITY");
            if (registeredAt <= 0 || globalClaimOrdinal < 1 || blockSize < 1)
                throw new IllegalArgumentException("PROTOCOL_ORDINAL_OR_TIME");
            finite(familyAlpha); finite(minimumCoverage); finite(minimumPracticalGain);
            if (!(familyAlpha > 0 && familyAlpha < 1) || minimumCoverage <= 0 || minimumCoverage > 1
                    || minimumPracticalGain < 0 || minimumPracticalGain >= 1)
                throw new IllegalArgumentException("PROTOCOL_RANGE");
            if (lookSchedule == null || lookSchedule.length == 0) throw new IllegalArgumentException("LOOK_SCHEDULE");
            this.lookSchedule = lookSchedule.clone();
            int prior = 0;
            for (int n : this.lookSchedule) {
                if (n <= prior) throw new IllegalArgumentException("LOOK_SCHEDULE_NOT_INCREASING");
                prior = n;
            }
            this.parent=parent; this.child=child; this.scope=scope; this.registeredAt=registeredAt;
            this.familyAlpha=familyAlpha; this.globalClaimOrdinal=globalClaimOrdinal;
            this.minimumCoverage=minimumCoverage; this.minimumPracticalGain=minimumPracticalGain;
            this.blockSize=blockSize;
            StringBuilder schedule=new StringBuilder();
            for (int n:this.lookSchedule) schedule.append(n).append(',');
            this.hash=digest("SU1_PAIRED_HOEFFDING_ALPHA_SPENDING_V1", this.claimId, this.familyRootHash, this.sourceEpoch,
                    parent.wire(), child.wire(), scope.name(), Long.toString(registeredAt),
                    Double.toHexString(familyAlpha), Integer.toString(globalClaimOrdinal),schedule.toString(),
                    Double.toHexString(minimumCoverage), Double.toHexString(minimumPracticalGain),Integer.toString(blockSize));
            for(int j=1;j<=this.lookSchedule.length;j++) alphaForLook(j);
        }
        public int lookCount() { return lookSchedule.length; }
        public int plannedAtLook(int look) { checkLook(look); return lookSchedule[look-1]; }
        public int maximumOpportunities() { return lookSchedule[lookSchedule.length-1]; }
        /** sum_{i>=1,j>=1} familyAlpha/[i(i+1)j(j+1)] = familyAlpha. */
        public double alphaForLook(int look) {
            checkLook(look);
            double i=globalClaimOrdinal, j=look;
            double a=familyAlpha/(i*(i+1.0)*j*(j+1.0));
            if (!(a>0) || !Double.isFinite(a)) throw new IllegalArgumentException("ALPHA_UNDERFLOW");
            return a;
        }
        private void checkLook(int look) {
            if(look<1||look>lookSchedule.length) throw new IllegalArgumentException("UNREGISTERED_LOOK");
        }
    }

    public static final class Opportunity {
        public final String targetIssue, eligibilityHash;
        public final long sequence, declaredAt, deadlineAt;
        public Opportunity(String issue,long sequence,long declaredAt,long deadlineAt,String eligibilityHash) {
            this.targetIssue=text(issue,"targetIssue"); this.eligibilityHash=sha(eligibilityHash);
            if(sequence<1||declaredAt<=0||deadlineAt<=declaredAt) throw new IllegalArgumentException("OPPORTUNITY_TIME");
            this.sequence=sequence;this.declaredAt=declaredAt;this.deadlineAt=deadlineAt;
        }
        private String wire() { return digest(targetIssue,Long.toString(sequence),Long.toString(declaredAt),Long.toString(deadlineAt),eligibilityHash); }
    }

    public static final class Freeze {
        public final String targetIssue,candidateId,protocolHash,policyHash,structureHash,runHash;
        public final String periodStateHash,inputHash,predictionHash;
        public final long capturedAt,inputThroughSequence,trainedThroughSequence;
        public final int position,numberMask;
        public Freeze(String issue,String candidateId,String protocolHash,String policyHash,
                String structureHash,String runHash,String periodStateHash,String inputHash,String predictionHash,
                long capturedAt,long inputThroughSequence,long trainedThroughSequence,int position,int n1,int n2,int n3) {
            this.targetIssue=text(issue,"targetIssue");this.candidateId=text(candidateId,"candidateId");
            this.protocolHash=sha(protocolHash);this.policyHash=sha(policyHash);this.structureHash=sha(structureHash);
            this.runHash=sha(runHash);this.periodStateHash=sha(periodStateHash);this.inputHash=sha(inputHash);
            this.predictionHash=sha(predictionHash);
            if(capturedAt<=0||inputThroughSequence<0||trainedThroughSequence<0||position<1||position>10
                    ||n1<1||n1>10||n2<1||n2>10||n3<1||n3>10||n1==n2||n1==n3||n2==n3)
                throw new IllegalArgumentException("FREEZE_DOMAIN");
            this.capturedAt=capturedAt;this.inputThroughSequence=inputThroughSequence;
            this.trainedThroughSequence=trainedThroughSequence;this.position=position;
            this.numberMask=(1<<(n1-1))|(1<<(n2-1))|(1<<(n3-1));
        }
        private String wire() { return digest(targetIssue,candidateId,protocolHash,policyHash,structureHash,runHash,
                periodStateHash,inputHash,predictionHash,Long.toString(capturedAt),Long.toString(inputThroughSequence),
                Long.toString(trainedThroughSequence),Integer.toString(position),Integer.toString(numberMask)); }
    }

    public static final class Settlement {
        public final String targetIssue,sourceEpoch,labelHash,settlementHash;
        public final long openedAt,completedAt;
        private final int[] permutation;
        public Settlement(String issue,String sourceEpoch,String labelHash,String settlementHash,
                long openedAt,long completedAt,int[] permutation) {
            this.targetIssue=text(issue,"targetIssue");this.sourceEpoch=sha(sourceEpoch);
            this.labelHash=sha(labelHash);this.settlementHash=sha(settlementHash);
            if(openedAt<=0||completedAt<openedAt||permutation==null||permutation.length!=10)
                throw new IllegalArgumentException("SETTLEMENT_DOMAIN");
            this.permutation=permutation.clone();int mask=0;
            for(int n:this.permutation){if(n<1||n>10||(mask&(1<<(n-1)))!=0)throw new IllegalArgumentException("DRAW_NOT_PERMUTATION");mask|=1<<(n-1);}
            this.openedAt=openedAt;this.completedAt=completedAt;
        }
        public int numberAt(int position) { if(position<1||position>10)throw new IllegalArgumentException("POSITION");return permutation[position-1]; }
        private String wire() {
            StringBuilder p=new StringBuilder();for(int x:permutation)p.append(x).append(',');
            return digest(targetIssue,sourceEpoch,labelHash,settlementHash,Long.toString(openedAt),Long.toString(completedAt),p.toString());
        }
    }

    public static final class Block {
        public final int firstOpportunity,lastOpportunity,paired;
        public final double pairedDelta,conservativeMean;
        private Block(int first,int last,int paired,int sum,int conservative) {
            firstOpportunity=first;lastOpportunity=last;this.paired=paired;
            pairedDelta=paired==0?Double.NaN:(double)sum/paired;
            conservativeMean=(double)conservative/(last-first+1);
        }
    }

    public static final class Result {
        public final int lookOrdinal,opportunities,paired,parentMissing,childMissing,unpaired;
        public final int parentHits,childHits,childOnlyWins,parentOnlyWins;
        /** Descriptive side totals, including rows whose counterpart is missing. */
        public final int parentCapturedHits,childCapturedHits;
        public final double pairedDelta,conservativeMean,lowerBound,coverage,allocatedAlpha,cumulativeAlphaSpent;
        public final String state,sourceChainHash,evidenceHash;
        public final List<Block> descriptiveBlocks;
        private Result(Protocol p,int look,int n,int pairs,int pm,int cm,int ph,int ch,int pch,int cch,int wins,int losses,
                int conservative,double spent,String chain,List<Block> blocks) {
            lookOrdinal=look;opportunities=n;paired=pairs;parentMissing=pm;childMissing=cm;
            unpaired=n-pairs;parentHits=ph;childHits=ch;childOnlyWins=wins;parentOnlyWins=losses;
            parentCapturedHits=pch;childCapturedHits=cch;
            pairedDelta=pairs==0?Double.NaN:(double)(wins-losses)/pairs;
            conservativeMean=(double)conservative/n;coverage=(double)pairs/n;allocatedAlpha=p.alphaForLook(look);
            cumulativeAlphaSpent=spent;
            lowerBound=Math.max(-1.0,conservativeMean-Math.sqrt(2.0*Math.log(1.0/allocatedAlpha)/n));
            state=coverage<p.minimumCoverage?"INSUFFICIENT_COVERAGE":lowerBound>p.minimumPracticalGain
                    ?"COMPARISON_CONFIRMED":look==p.lookCount()?"EXHAUSTED_NOT_CONFIRMED":"CONTINUE";
            sourceChainHash=chain;descriptiveBlocks=Collections.unmodifiableList(new ArrayList<Block>(blocks));
            evidenceHash=digest(p.hash,Integer.toString(look),Integer.toString(n),Integer.toString(pairs),
                    Integer.toString(pm),Integer.toString(cm),Integer.toString(ph),Integer.toString(ch),
                    Integer.toString(pch),Integer.toString(cch),
                    Integer.toString(wins),Integer.toString(losses),Double.toHexString(conservativeMean),
                    Double.toHexString(lowerBound),Double.toHexString(allocatedAlpha),Double.toHexString(spent),chain,state);
        }
        /** Comparison evidence only: this class never authorizes production influence. */
        public boolean mayAuthorizeReal() { return false; }
    }

    private static final class Slot {
        final Opportunity opportunity;Freeze parent,child;Settlement settlement;
        Slot(Opportunity opportunity){this.opportunity=opportunity;}
    }
    public final Protocol protocol;
    private final LinkedHashMap<String,Slot> slots=new LinkedHashMap<String,Slot>();
    private final ArrayList<Result> results=new ArrayList<Result>();
    private double spent;
    private String integrityFailure="";
    public PairedEvidence(Protocol protocol){if(protocol==null)throw new IllegalArgumentException("PROTOCOL");this.protocol=protocol;}
    public boolean quarantined(){return !integrityFailure.isEmpty();}
    public String integrityFailure(){return integrityFailure;}
    public double alphaSpent(){return spent;}

    public synchronized boolean declare(Opportunity o) {
        healthy();if(o==null)throw new IllegalArgumentException("OPPORTUNITY");
        Slot old=slots.get(o.targetIssue);
        if(old!=null){if(old.opportunity.wire().equals(o.wire()))return false;throw conflict("OPPORTUNITY_REPLAY_CONFLICT");}
        if(o.declaredAt<protocol.registeredAt||slots.size()>=protocol.maximumOpportunities())
            throw new IllegalArgumentException("OUTSIDE_REGISTERED_SCHEDULE");
        if(!slots.isEmpty()){
            Slot last=null;for(Slot s:slots.values())last=s;
            if(o.sequence<=last.opportunity.sequence)throw new IllegalArgumentException("SEQUENCE_NOT_INCREASING");
        }
        slots.put(o.targetIssue,new Slot(o));return true;
    }

    public synchronized boolean freeze(Freeze f) {
        healthy();if(f==null)throw new IllegalArgumentException("FREEZE");
        Slot s=required(f.targetIssue);Identity identity;
        boolean parent=f.candidateId.equals(protocol.parent.candidateId);
        if(parent)identity=protocol.parent;
        else if(f.candidateId.equals(protocol.child.candidateId))identity=protocol.child;
        else throw new IllegalArgumentException("CANDIDATE_NOT_REGISTERED");
        if(!protocol.hash.equals(f.protocolHash)||!identity.policyHash.equals(f.policyHash)
                ||!identity.structureHash.equals(f.structureHash)||!identity.runHash.equals(f.runHash))
            throw new IllegalArgumentException("PROTOCOL_OR_MODEL_IDENTITY_MISMATCH");
        Freeze old=parent?s.parent:s.child;
        if(old!=null){if(old.wire().equals(f.wire()))return false;throw conflict("FREEZE_REPLAY_CONFLICT");}
        if(s.settlement!=null)throw new IllegalArgumentException("POST_SETTLEMENT_FREEZE");
        if(f.capturedAt<s.opportunity.declaredAt||f.capturedAt>=s.opportunity.deadlineAt)
            throw new IllegalArgumentException("FREEZE_AFTER_DEADLINE_OR_BEFORE_DECLARATION");
        if(f.inputThroughSequence>=s.opportunity.sequence||f.trainedThroughSequence>=s.opportunity.sequence)
            throw new IllegalArgumentException("FUTURE_INPUT_OR_TRAINING");
        Freeze other=parent?s.child:s.parent;
        if(protocol.scope==Scope.SAME_POSITION_K3&&other!=null&&other.position!=f.position)
            throw new IllegalArgumentException("POSITION_SCOPE_MISMATCH");
        if(parent)s.parent=f;else s.child=f;return true;
    }

    public synchronized boolean settle(Settlement settlement) {
        healthy();if(settlement==null)throw new IllegalArgumentException("SETTLEMENT");
        Slot s=required(settlement.targetIssue);
        if(s.settlement!=null){if(s.settlement.wire().equals(settlement.wire()))return false;throw conflict("SETTLEMENT_REPLAY_CONFLICT");}
        if(!protocol.sourceEpoch.equals(settlement.sourceEpoch))throw new IllegalArgumentException("SOURCE_EPOCH_MISMATCH");
        if(settlement.openedAt<s.opportunity.deadlineAt)throw new IllegalArgumentException("DRAW_BEFORE_DECLARED_CUTOFF");
        s.settlement=settlement;return true;
    }

    /** Returns null until the entire predeclared opportunity prefix has canonical outcomes.
     * Calling twice for the same look is idempotent and does not spend alpha again.
     */
    public synchronized Result look(int lookOrdinal) {
        healthy();int n=protocol.plannedAtLook(lookOrdinal);
        if(lookOrdinal<=results.size())return results.get(lookOrdinal-1);
        if(lookOrdinal!=results.size()+1)throw new IllegalArgumentException("LOOK_ORDER");
        if(slots.size()<n)return null;
        ArrayList<Slot> prefix=new ArrayList<Slot>(n);for(Slot s:slots.values()){if(prefix.size()==n)break;if(s.settlement==null)return null;prefix.add(s);}
        int paired=0,pm=0,cm=0,ph=0,ch=0,pch=0,cch=0,wins=0,losses=0,conservative=0;
        int blockPairs=0,blockSum=0,blockConservative=0,blockFirst=1;
        ArrayList<Block> blocks=new ArrayList<Block>();String chain=protocol.hash;
        for(int index=0;index<prefix.size();index++){
            Slot s=prefix.get(index);int p=s.parent==null?0:hit(s.parent,s.settlement),c=s.child==null?0:hit(s.child,s.settlement);
            pch+=p;cch+=c;if(s.parent==null)pm++;if(s.child==null)cm++;
            int d;
            if(s.parent!=null&&s.child!=null){paired++;ph+=p;ch+=c;blockPairs++;d=c-p;blockSum+=d;if(d==1)wins++;if(d==-1)losses++;}
            else d=-1;
            conservative+=d;blockConservative+=d;
            chain=digest(chain,s.opportunity.wire(),s.parent==null?"MISSING_PARENT":s.parent.wire(),
                    s.child==null?"MISSING_CHILD":s.child.wire(),s.settlement.wire());
            int count=index+1;
            if(count%protocol.blockSize==0||count==n){blocks.add(new Block(blockFirst,count,blockPairs,blockSum,blockConservative));blockFirst=count+1;blockPairs=blockSum=blockConservative=0;}
        }
        double nextSpent=spent+protocol.alphaForLook(lookOrdinal);
        Result result=new Result(protocol,lookOrdinal,n,paired,pm,cm,ph,ch,pch,cch,wins,losses,conservative,nextSpent,chain,blocks);
        // Production adapter must commit look row + alpha debit in ONE transaction before publishing.
        results.add(result);spent=nextSpent;return result;
    }

    private static int hit(Freeze f,Settlement s){return (f.numberMask&(1<<(s.numberAt(f.position)-1)))!=0?1:0;}
    private Slot required(String issue){Slot s=slots.get(issue);if(s==null)throw new IllegalArgumentException("UNDECLARED_OPPORTUNITY");return s;}
    private void healthy(){if(quarantined())throw new IllegalStateException("CLAIM_QUARANTINED:"+integrityFailure);}
    private IllegalStateException conflict(String reason){integrityFailure=reason;return new IllegalStateException(reason);}
    private static void finite(double n){if(!Double.isFinite(n))throw new IllegalArgumentException("NONFINITE");}
    private static String text(String s,String name){if(s==null||s.trim().isEmpty())throw new IllegalArgumentException(name);return s;}
    private static String sha(String s){if(s==null||!s.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("SHA256_REQUIRED");return s;}
    public static String digest(String... fields){
        try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
            for(String s:fields){byte[] b=text(s,"DIGEST_FIELD").getBytes(StandardCharsets.UTF_8);out.writeInt(b.length);out.write(b);}out.flush();
            byte[] hash=MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray());StringBuilder hex=new StringBuilder(64);
            for(byte b:hash)hex.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return hex.toString();
        }catch(java.io.IOException|java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
}
