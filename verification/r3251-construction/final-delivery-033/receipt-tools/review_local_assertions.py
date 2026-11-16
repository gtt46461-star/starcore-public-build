#!/usr/bin/env python3
"""Individually reviewed LOCAL predicates, bound to executed owner-stage XML."""
import hashlib,json,subprocess
from pathlib import Path
ROOT=Path('/workspace/starcore-r3251-attachment-final/verification/r3251-construction/final-delivery-033')
REVIEWS={
 'R31-B00-C02#A2':('R31InjectedCauseChainContractTest','fiveInjectedCausesHaveDistinctCategoriesAndDispositions','The executed method constructs nested SQLite busy, stale owner, SQL syntax, full disk and user cancellation separately. It asserts five distinct production failure categories/dispositions and exactly one fault-budget consumer. The original clause asks for these injected classifications, not physical failure-rate evidence.'),
 'R31-B11-C01#A3':('R31MetricReportTest','everyMetricHasMissingZeroFailureAndSufficientFixturesAndNaturalShortageIsReported','The executed loop evaluates each of all 44 registered metrics with missing source, zero samples, invalid finite-value cases, sufficient failure/success and below-minimum cases. It asserts unknown values stay null and synthetic/natural roles remain distinct. The original clause explicitly asks for fixture behavior, not sufficient actual natural data.'),
 'R31-B11-C01#A4':('R31MetricReportTest','crossSourceWatermarkConflictsAreDetectedAndLegalDifferentScopesAreNot','The executed fixture asserts stale/scope mismatch on the historical zero-conflict example, distinguishes incomparable roles, accepts agreeing same-role/scope/watermark values, detects a same-scope value contradiction, and rejects missing owner fields.'),
 'R31-B11-C02#A1':('R31MetricReportTest','everyChartMatrixAndCapabilityHasItsOwnEntry','The executed production object-index call enumerates unique per-object entries, asserts 47 charts,20 matrices and the complete capability registry count, and preserves UNKNOWN/NOT_PASS for unavailable runtime facts.'),
 'R31-B11-C02#A2':('R31MetricReportTest','runningWithoutCommitEmptyFrameAndConsumedWithoutAdvantageAreDistinct','The executed production layer evaluator separately asserts RUNNING_NO_COMMIT, EMPTY, and consumed YES with NO_MEASURED_ADVANTAGE/NOT_PASS. This matches each of the three original counterexamples.'),
 'R31-B11-C03#A1':('R31MetricReportTest','anInsufficientBudgetListsOmittedItemsAndLeavesTheirMetricsUnknown','The executed bounded attachment plan actually creates two files, includes one, omits the oversized item, leaves its metric UNKNOWN_ATTACHMENT_NOT_INCLUDED, and asserts display truncation is not the denominator or a complete raw package.'),
 'R31-B11-C04#A1':('R31MetricReportTest','checkIdsEqualTheReal96AndEveryMetricHasIndependentEvidence','The executed method reads the original checks_96.json, checks exact 96-ID set and preserved domain outcomes, asserts 44 metric definitions with nonempty independent evidence requirements, and explicitly retains unsupported conditions.'),
 'R31-B11-C04#A3':('R31MetricReportTest','completeMetadataDoesNotMeanTheProductPassed','The executed production coverage evaluator asserts productAllPassed=false for check metadata and again for 100% report/metadata coverage; reported passRate remains zero for merely supported evidence.')}

def main():
 rows=json.loads((ROOT/'CURRENT_ACCEPTANCE_222_EXECUTION.json').read_text());by={x['id']:x for x in rows}
 stages={x['batch']:x for x in json.loads((ROOT/'FIVE_STAGE_RESULTS.json').read_text())};proof=[]
 repo=ROOT.parents[2]
 for id,(cls,method,reason) in REVIEWS.items():
  row=by[id];assert row['level']=='LOCAL';stage=stages[row['currentBatch']]
  entry='com.ethk3.novaonepro.strategyv2.'+cls+'#'+method
  refs=[x for x in row['engineeringEntryResults'] if x['entry']==entry];assert len(refs)==1 and refs[0]['status']=='PASS'
  ref=refs[0];xml=ROOT/ref['evidenceRoot']/ref['xml'];actual=hashlib.sha256(xml.read_bytes()).hexdigest()
  assert actual==ref.get('sha256',ref.get('xmlSha256'))
  path='app/src/test/java/com/ethk3/novaonepro/strategyv2/'+cls+'.java'
  source=subprocess.check_output(['git','show',stage['sourceCommit']+':'+path],cwd=repo)
  assert ('void '+method+'(').encode() in source
  item={'id':id,'originalTextSha256':row['original_text_sha256'],'scope':'EXACT_ORIGINAL_LOCAL_FIXTURE_OR_REGISTRY_PREDICATE_NOT_DEVICE_OR_NATURAL','ownStage':row['currentBatch'],'sourceCommit':stage['sourceCommit'],'testSourcePath':path,'testSourceSha256':hashlib.sha256(source).hexdigest(),'executedEntry':entry,'actualXml':xml.relative_to(ROOT).as_posix(),'xmlSha256':actual,'individualAssertionReview':reason,'result':'PASS_EXACT_ORIGINAL_LOCAL_CLAUSE'}
  proof.append(item);row.update(wholeClauseProved=True,individualClauseProof='INDIVIDUAL_LOCAL_ASSERTION_REVIEWS.json#'+id,remaining='None for this exact original LOCAL fixture/registry clause. Actual Android, physical and natural clauses remain separate.')
 (ROOT/'INDIVIDUAL_LOCAL_ASSERTION_REVIEWS.json').write_text(json.dumps(proof,ensure_ascii=False,indent=2)+'\n')
 (ROOT/'CURRENT_ACCEPTANCE_222_EXECUTION.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2)+'\n')
 p=ROOT/'DELIVERY_STATUS.json';status=json.loads(p.read_text());n=sum(x['wholeClauseProved'] for x in rows)
 status.update(wholeOriginalClausesIndividuallySigned=n,wholeOriginalClausesNotYetSigned=222-n);p.write_text(json.dumps(status,ensure_ascii=False,indent=2)+'\n')
 print('INDIVIDUAL_LOCAL_ASSERTION_REVIEW_PASS',len(proof),'wholeOriginalSigned',n,'noGroupPassImported',True)

if __name__=='__main__':main()
