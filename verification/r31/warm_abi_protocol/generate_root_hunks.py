#!/usr/bin/env python3
"""Emit reviewed changes for root-owned callers; never edit their production files."""
from pathlib import Path
import difflib,hashlib,json
ROOT=Path(__file__).resolve().parents[3]
DIRECTORY=Path(__file__).resolve().parent
PACKAGE=Path('app/src/main/java/com/ethk3/novaonepro/strategyv2')
def required(text,old,new):
    if text.count(old)!=1:raise ValueError('Exact root hunk anchor changed: '+old[:100])
    return text.replace(old,new)
def patched_sources():
    results={}
    path=PACKAGE/'FoundryCampaignR26.java';old=(ROOT/path).read_text();new=old
    start='''        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_CAMPAIGN_ABI_GUARD",db->{RuntimeCoordinatorR26.requireFence(db,claims.get());db.execSQL("UPDATE r26_foundry SET status='ABANDONED_NUMERICAL_ABI',snapshot=X'' WHERE status='RUNNING' AND numerical_abi<>?",new Object[]{numericalAbi});WarmCandidateR26.preserveExpired(db);return 0;});'''
    replacement='''        try(WarmCandidateR26.PreparedAbiR31 abi=WarmCandidateR26.prepareAbiR31(store.readable(),numericalAbi);
            WarmCandidateR26.PreparedPreservationR31 warm=WarmCandidateR26.preparePreservationR31(store.readable(),abi.campaigns())){
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_CAMPAIGN_ABI_GUARD",db->{RuntimeCoordinatorR26.requireFence(db,claims.get());WarmCandidateR26.applyPreparedAbiR31(db,abi);WarmCandidateR26.applyPreparedPreservationR31(db,warm);RuntimeCoordinatorR26.requireFence(db,claims.get());return 0;});
        }'''
    new=required(new,start,replacement)
    start='''        if(latestOrdinal-Snapshot.decode(snapshotBytes).ordinal[0]>=5000){store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_CAMPAIGN_EXPIRE",db->{RuntimeCoordinatorR26.requireFence(db,claims.get());db.execSQL("UPDATE r26_foundry SET status='EXPIRED_RETENTION',snapshot=X'' WHERE campaign_id=?",new Object[]{id});WarmCandidateR26.preserveExpired(db);return 0;});return 0;}'''
    replacement='''        if(latestOrdinal-Snapshot.decode(snapshotBytes).ordinal[0]>=5000){try(WarmCandidateR26.PreparedPreservationR31 warm=WarmCandidateR26.preparePreservationR31(store.readable(),Collections.singletonList(id))){store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_CAMPAIGN_EXPIRE",db->{RuntimeCoordinatorR26.requireFence(db,claims.get());db.execSQL("UPDATE r26_foundry SET status='EXPIRED_RETENTION',snapshot=X'' WHERE campaign_id=?",new Object[]{id});WarmCandidateR26.applyPreparedPreservationR31(db,warm);RuntimeCoordinatorR26.requireFence(db,claims.get());return 0;});}return 0;}'''
    new=required(new,start,replacement)
    new=required(new,'ModelPublicationR31.stageAndPublish(store,claims.get(),plan,FoundryCampaignR26::publishBirthPreparedR31);','ModelPublicationR31.stageAndPublish(store,claims.get(),plan);')
    new=required(new,'ModelPublicationR31.stageAndPublish(store,claims.get(),plan,FoundryCampaignR26::publishFinishPreparedR31);','ModelPublicationR31.stageAndPublish(store,claims.get(),plan);')
    results[path]=(old,new)
    path=PACKAGE/'LegacyGenerationR26.java';old=(ROOT/path).read_text();new=required(old,'ModelPublicationR31.stageAndPublish(store,claim,plan,LegacyGenerationR26::publishPreparedR31);','ModelPublicationR31.stageAndPublish(store,claim,plan);');results[path]=(old,new)
    path=PACKAGE/'RuntimeCoordinatorR26.java';old=(ROOT/path).read_text();new=old
    start='''        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.RETENTION,"R26_RETENTION_PHASE_"+checkpoint.phase),db->{'''
    replacement='''        try(WarmCandidateR26.PreparedPreservationR31 warm=checkpoint.phase==4?WarmCandidateR26.preparePreservationR31(read):null;
            WarmCandidateR26.PreparedInitializerPruneR31 initializers=checkpoint.phase==5?WarmCandidateR26.prepareInitializerPruneR31(read):null){
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.RETENTION,"R26_RETENTION_PHASE_"+checkpoint.phase),db->{'''
    new=required(new,start,replacement)
    start='''            else if(checkpoint.phase<RetentionR26.PHASES_R30_REPAIR+2)RetentionR26.pruneStep(db,cycle+1,checkpoint.phase-2);'''
    replacement='''            else if(checkpoint.phase==4)WarmCandidateR26.applyPreparedPreservationR31(db,warm);
            else if(checkpoint.phase==5)WarmCandidateR26.applyPreparedInitializerPruneR31(db,initializers);
            else if(checkpoint.phase<RetentionR26.PHASES_R30_REPAIR+2)RetentionR26.pruneStep(db,cycle+1,checkpoint.phase-2);'''
    new=required(new,start,replacement)
    start='''            RetentionR26.commitCheckpoint(db,checkpoint,cycle,fold==null?null:fold.stepHash,fold==null?-1:fold.throughOrdinal);return 0;'''
    replacement='''            requireFence(db,claim);
            if((warm==null||!warm.more)&&(initializers==null||!initializers.more))RetentionR26.commitCheckpoint(db,checkpoint,cycle,fold==null?null:fold.stepHash,fold==null?-1:fold.throughOrdinal);return 0;'''
    new=required(new,start,replacement)
    start='''        // Legacy task cursor is a work counter here; typed progress in B03 separates it from the calendar.'''
    new=required(new,start,'''        }
'''+start);results[path]=(old,new)
    return results
if __name__=='__main__':
    result=patched_sources();diff='';facts={}
    for path,(old,new) in result.items():
        diff+=''.join(difflib.unified_diff(old.splitlines(True),new.splitlines(True),fromfile='a/'+str(path),tofile='b/'+str(path)))
        facts[str(path)]={'originalSha256':hashlib.sha256(old.encode()).hexdigest(),'patchedSha256':hashlib.sha256(new.encode()).hexdigest()}
    (DIRECTORY/'ROOT_CALLERS.patch').write_text(diff)
    (DIRECTORY/'ROOT_CALLERS.json').write_text(json.dumps({'scope':'PROPOSED_ROOT_OWNED_CALLERS_NOT_APPLIED_TO_SHARED_SOURCE','base':'503efdc2c7abdb78b6e30fbd9d881d8dc772265e','sources':facts},indent=2)+'\n')
