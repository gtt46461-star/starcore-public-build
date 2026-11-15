#!/usr/bin/env python3
from pathlib import Path

p=Path('app/src/main/java/com/ethk3/novaonepro/CertifiedResearchMixerR915.java')
text=p.read_text(encoding='utf-8')

# The current unified branch already uses the stronger typed molecule trace quarantine guard.
guard='''            if(GenesisMoleculeTraceR932.blockedByQuarantine(identity,true,AtomCaptureR932.quarantined(store)))\n                return fallback(store,productionActions,"ASSET_PARENT_ATOM_QUARANTINED_ZERO_CONTRIBUTION",candidate);\n'''
if guard not in text: raise SystemExit('E04G_TYPED_MOLECULE_QUARANTINE_GUARD_MISSING')

anchor='''    public static int revoke(DbStore store,String assetId,String reason) {\n'''
helper='''    /** A zero-contribution/fallback result is forbidden from changing the production ranking in memory. */\n    static void assertZeroContributionInvariant(Result result,V51Models.PositionAction[] productionActions) {\n        if(result==null||result.applied||Double.doubleToLongBits(result.influenceWeight)!=Double.doubleToLongBits(0.0d)\n                ||result.contributionHash.length()!=0||result.actions!=productionActions)\n            throw new IllegalStateException("R932_ZERO_CONTRIBUTION_MUTATED_RANKING");\n        V51Models.PositionAction expected=null;\n        if(productionActions!=null)for(V51Models.PositionAction action:productionActions)\n            if(action!=null&&action.rank==1){expected=action;break;}\n        if((expected==null)!=(result.rankOne==null)\n                ||(expected!=null&&!expected.checksum.equals(result.rankOne.checksum)))\n            throw new IllegalStateException("R932_ZERO_CONTRIBUTION_RANK1_DRIFT");\n    }\n\n'''
if helper not in text:
    if text.count(anchor)!=1: raise SystemExit('E04G_HELPER_ANCHOR')
    text=text.replace(anchor,helper+anchor,1)

old_fb='''    private static Result fallback(DbStore store,V51Models.PositionAction[] actions,String status,Candidate c){\n        V51Models.PositionAction rankOne=null;if(actions!=null)for(V51Models.PositionAction a:actions)\n            if(a!=null&&a.rank==1){rankOne=a;break;}Result r=new Result(false,actions,rankOne,status,c,0,"");\n        if(store!=null)persistTruth(store,r);return r;}\n'''
new_fb='''    private static Result fallback(DbStore store,V51Models.PositionAction[] actions,String status,Candidate c){\n        V51Models.PositionAction rankOne=null;if(actions!=null)for(V51Models.PositionAction a:actions)\n            if(a!=null&&a.rank==1){rankOne=a;break;}Result r=new Result(false,actions,rankOne,status,c,0,"");\n        assertZeroContributionInvariant(r,actions);\n        if(store!=null)persistTruth(store,r);return r;}\n'''
if new_fb not in text:
    if text.count(old_fb)!=1: raise SystemExit('E04G_FALLBACK_ANCHOR')
    text=text.replace(old_fb,new_fb,1)

old_truth='''        s.put("r915_asset_mixer_status",r.status);s.put("r915_asset_mixer_id",r.assetId);\n        s.put("r915_asset_mixer_name_zh",r.nameZh);s.put("r915_asset_mixer_proof",r.proof());\n        s.put("r915_asset_mixer_at",String.valueOf(System.currentTimeMillis()));store.setStates(s);}catch(Throwable ignored){}}\n'''
new_truth='''        s.put("r915_asset_mixer_status",r.status);s.put("r915_asset_mixer_id",r.assetId);\n        s.put("r915_asset_mixer_name_zh",r.nameZh);s.put("r915_asset_mixer_proof",r.proof());\n        s.put("r915_asset_mixer_applied",r.applied?"1":"0");\n        s.put("r915_asset_mixer_weight",String.valueOf(r.influenceWeight));\n        s.put("r915_asset_mixer_rank_changed",r.applied?"EVALUATED_IN_CONTRIBUTION_LEDGER":"0");\n        s.put("r915_asset_mixer_at",String.valueOf(System.currentTimeMillis()));store.setStates(s);}catch(Throwable ignored){}}\n'''
if new_truth not in text:
    if text.count(old_truth)!=1: raise SystemExit('E04G_TRUTH_ANCHOR')
    text=text.replace(old_truth,new_truth,1)

p.write_text(text,encoding='utf-8')
print('E04G_TYPED_PRODUCTION_GUARD_AND_ZERO_CONTRIBUTION_INVARIANT_APPLIED=PASS')
