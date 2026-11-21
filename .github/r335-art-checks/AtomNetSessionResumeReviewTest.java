package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/** Actual numerical owner; in-memory samples isolate the checkpoint/report contract. */
public final class AtomNetSessionResumeReviewTest {
    static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    static AtomNetSessionR30 copy(AtomNetSessionR30 value) {
        byte[] bytes = value.encode();
        return AtomNetSessionR30.decode(bytes, StatsR27.sha256(bytes));
    }
    static boolean accepts(AtomNetSessionR30 value) {
        try { copy(value); return true; }
        catch (IllegalArgumentException expected) { return false; }
    }
    public static void main(String[] args) {
        boolean fixed = args.length == 1 && args[0].equals("fixed");
        final int train = 1600, select = 80;
        AtomNetSessionR30 session = new AtomNetSessionR30(StatsR27.sha256("R335_B04_REVIEW"),
                "202611160200", 200, 17, train, select, 40);
        session.materializedSamples = train + select;
        session.cachePeriod = 24 + (train + select) / 10;
        require(((Number)session.report().get("independentTrainingCalendarIssues")).longValue()==0,
                "zero gradients cannot count training issues");
        AtomNetSessionR30.Samples samples = index -> AtomNetSessionNumericalProbe.sample(index,
                String.format(Locale.ROOT, "20261116%04d", 25 + index / 10));
        int consumed = session.work(samples);
        require(consumed == 64 && session.pretrainSamples == 64 && session.offset == 64,
                "must observe an actual first Adam slice");
        Set<Integer> actual = new HashSet<>();
        for (int i=0;i<session.offset;i++) actual.add(session.order[i]/10);
        long reported = ((Number)session.report().get("independentTrainingCalendarIssues")).longValue();
        require(actual.size() <= 64 && actual.size() < train/10, "fixture distinguishes visit count");
        require(fixed ? reported==actual.size() : reported==train/10,
                "first-slice report result does not match expected revision");
        require(Arrays.equals(session.encode(),copy(session).encode()), "valid checkpoint is lossless");
        AtomNetSessionR30 corrupt = copy(session);
        corrupt.order = null;
        boolean missingOrderAccepted = accepts(corrupt);
        require(missingOrderAccepted != fixed, "missing shuffle cursor rejection");
        boolean divergence = false;
        if (!fixed) {
            AtomNetSessionR30 badResume = copy(corrupt), originalResume = copy(session);
            badResume.work(samples); originalResume.work(samples);
            divergence = !Arrays.equals(badResume.trainer.net.encode(),originalResume.trainer.net.encode());
            require(divergence, "accepted corrupt resume must reproduce numerical divergence");
        }
        AtomNetSessionR30 completedTrainCursor = copy(session);
        completedTrainCursor.offset = train;
        require(accepts(completedTrainCursor) != fixed, "terminal TRAIN cursor must not resume");
        AtomNetSessionR30 completedEpochCursor = copy(session);
        completedEpochCursor.epoch = AtomNetTrainerR27.MAX_EPOCHS;
        require(accepts(completedEpochCursor) != fixed, "terminal TRAIN epoch must not resume");
        if (fixed) {
            AtomNetSessionR30 afterRestart = copy(session);
            require(((Number)afterRestart.report().get("independentTrainingCalendarIssues")).longValue()
                    ==actual.size(), "distinct visit count survives restart");
            Set<Integer> second = new HashSet<>(actual);
            afterRestart.work(samples);
            for (int i=0;i<afterRestart.offset;i++) second.add(afterRestart.order[i]/10);
            require(((Number)afterRestart.report().get("independentTrainingCalendarIssues")).longValue()
                    ==second.size(), "second slice counts distinct issues, not visits");
            while ("TRAIN".equals(afterRestart.phase)) afterRestart.work(samples);
            require(((Number)afterRestart.report().get("independentTrainingCalendarIssues")).longValue()
                    ==train/10, "full first epoch covers all historical training issues");
            require(accepts(afterRestart), "valid completed epoch remains resumable");
            // Production splits are multiples of ten, but the public numerical
            // session also accepts a partial final issue. Count that issue once.
            AtomNetSessionR30 partialIssueCount = new AtomNetSessionR30(StatsR27.sha256("PARTIAL_TRAIN"),
                    "202611160200",200,17,69,11,0);
            partialIssueCount.materializedSamples=80;partialIssueCount.cachePeriod=32;
            partialIssueCount.work(samples);
            Set<Integer> partialVisited=new HashSet<>();
            for(int i=0;i<partialIssueCount.offset;i++)partialVisited.add(partialIssueCount.order[i]/10);
            require(((Number)partialIssueCount.report().get("independentTrainingCalendarIssues")).longValue()
                    ==partialVisited.size(),"non-multiple split first batch exact distinct count");
            partialIssueCount.work(samples);
            require(((Number)partialIssueCount.report().get("independentTrainingCalendarIssues")).longValue()
                    ==7,"69 position samples cover seven historical issues");
            require(accepts(partialIssueCount),"partial final issue does not corrupt numerical checkpoint");
            AtomNetSessionR30 unknown=copy(session);unknown.epoch=1;
            require(unknown.report().get("independentTrainingCalendarIssues")==null,
                    "inconsistent short history in later epoch remains unknown");
            AtomNetSessionR30 onlineCounter=copy(session);onlineCounter.trainer.onlineSteps=10;
            require(((Number)onlineCounter.report().get("independentTrainingCalendarIssues")).longValue()
                    ==actual.size(),"online samples do not inflate historical TRAIN issue count");
        }
        System.out.println("{\"mode\":\""+(fixed?"fixed":"legacy")+"\",\"actualDistinctIssues\":"
                +actual.size()+",\"reportedIssues\":"+reported+",\"pretrainVisits\":64,"
                +"\"missingShuffleAccepted\":"+missingOrderAccepted+",\"restoredWeightsDiverged\":"
                +divergence+",\"result\":\"PASS_EXPECTED_REVISION_BEHAVIOR\"}");
    }
}
