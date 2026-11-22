    @Test public void observedCoverageFailurePersistsTwoBirthsAndTrainsTheDistinctChild() throws Exception {
        try (Fixture f = new Fixture(128)) {
            FoundryCampaignR26.Snapshot source = FoundryCampaignR26.capture(f.store);
            String snapshot = LiveStateR26.hash(source.encode());
            long from = source.ordinal[0] - QualificationWindowR26.LENGTH;
            String digest = PolicyIdentityR26.sha256("CONTROLLED_COVERAGE_FAILURE");
            ModelGenomeSU2 seed = FamilyRegistryR26.genome("F03");
            ModelGenomeSU2 parent = new ModelGenomeSU2(seed.members, seed.pool, seed.sink, seed.gate,
                    0.15, seed.metaC, seed.calibrated).bindBirth(PolicyIdentityR26.sha256("CONTROLLED_PRIOR_SNAPSHOT"), from - 1);
            byte[] initial = new LiveStateR26(parent).encode();
            f.store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_REVEALED_COVERAGE", writer -> {
                writer.execSQL("INSERT INTO r26_live_model(policy_hash,canonical_text,state,state_hash,checkpoint_hash,parent_hash,through_ordinal,genesis_ordinal,born_ordinal,updated_at,family_id) VALUES(?,?,?,?,?,'',?,?,?,1,'F03')",
                        new Object[]{parent.key(), parent.text(), initial, LiveStateR26.hash(initial), "CONTROLLED", source.through, from, from});
                writer.execSQL("INSERT INTO r26_cohort(genome_key,from_sequence,to_sequence,incumbent,protocol,family_alpha) VALUES(?,?,?,'CONTROLLED',?,0.01)",
                        new Object[]{parent.key(), from, from + QualificationWindowR26.LENGTH, QualificationWindowR26.PROTOCOL});
                writer.execSQL("INSERT INTO r26_qualification_result VALUES(?,?,?,1,?,2000,600,0.3,0.25,?,0.3,-0.02,1500,500,0.02,'COVERAGE_LT_0.15',1)",
                        new Object[]{parent.key(), from, from + QualificationWindowR26.LENGTH, digest, 2000.0 / QualificationWindowR26.LENGTH});
                return 0;
            });
            assertTrue(FailureDirectedResearchR26.select(f.db, snapshot, source.ordinal[0] - 2).isEmpty());
            List<FailureDirectedResearchR26.Batch> batches = FailureDirectedResearchR26.select(f.db, snapshot, source.through, source.draws.length);
            assertEquals(1, batches.size());
            List<StructuralProposalR26.Proposal> children = batches.get(0).proposals;
            assertEquals(2, children.size());
            assertEquals("NO_OP", children.get(0).category);
            assertEquals("STRUCTURAL_CANDIDATE", children.get(1).category);
            String child = children.get(1).genome.key();
            assertNotEquals(parent.key(), child);
            assertTrue(children.get(1).genome.coverage > parent.coverage);
            for (int i = 0; i < 180 && scalar(f.db, "SELECT count(*) FROM r26_failure_research_cursor") == 0; i++)
                FoundryCampaignR26.step(f.store, () -> f.claim, f.budget());
            try (Cursor c = f.db.rawQuery("SELECT c.campaign_id,c.proposal_json FROM r26_failure_research_cursor c WHERE parent_policy=? AND evidence_digest=?", new String[]{parent.key(),digest})) {
                assertTrue(c.moveToFirst());
                assertEquals(2,new org.json.JSONObject(c.getString(1)).getJSONArray("children").length());
                try (Cursor pairs = f.db.rawQuery("SELECT count(*) FROM r26_candidate c JOIN r26_evolution e ON c.campaign_id=e.campaign_id AND e.policy_hash=c.policy_hash WHERE c.campaign_id=? AND e.parent=? AND e.operation LIKE 'FAILURE_GUIDED_V1:%'",new String[]{c.getString(0),parent.key()})) {
                    assertTrue(pairs.moveToFirst()); assertEquals(2,pairs.getInt(0));
                }
            }
            assertTrue(FailureDirectedResearchR26.select(f.db, snapshot, source.through).isEmpty());
            f.store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("CONTROLLED_ISOLATE_REAL_TRAINER", tx -> {
                tx.execSQL("UPDATE r26_candidate SET status='DONE' WHERE policy_hash<>?",new Object[]{child});
                return 0;
            });
            assertTrue(FoundryCampaignR26.step(f.store, () -> f.claim, f.budget()) > 0);
            assertTrue(scalar(f.db, "SELECT cursor FROM r26_candidate WHERE policy_hash='" + child + "'") > 0);
            assertTrue(scalar(f.db, "SELECT count(*) FROM r26_training_step WHERE policy_hash='" + child + "'") > 0);
            assertTrue(TrainingStepStoreR26.verify(f.db));
            assertEquals(1, scalar(f.db, "SELECT count(*) FROM r26_cohort"));
            assertEquals(0, scalar(f.db, "SELECT count(*) FROM su2_certificate"));
            assertEquals(0, scalar(f.db, "SELECT sum(prospective_qualified+verified_improvement) FROM r26_evolution"));
        }
    }
