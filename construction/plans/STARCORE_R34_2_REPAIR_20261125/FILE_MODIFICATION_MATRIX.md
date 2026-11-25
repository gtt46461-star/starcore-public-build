# 问题—批次—文件—方法修改矩阵

每个路径只有一个顺序变更链；本表包含生产/方案及本批拟新增验收文件，方法级增量施工，不提供未经编译验证的整文件覆盖补丁。FILE_CARDS.json的issue_scopes逐项保留各批动作，method_contracts区分既有符号和拟新增符号；文件存在不表示新增方法已实现。只读验证项不得被误当强制重写。

|文件|基线存在|批次顺序|问题|方法/入口/字段|
|---|---|---|---|---|
|`.github/workflows/build-apk.yml`|True|B01|R342-002|jobs.debug-apk, jobs.r342-candidate-build, jobs.r342-publish, on.workflow_dispatch|
|`.github/workflows/r31-publish-release.yml`|True|B01|R342-002|jobs.publish, jobs.publish.steps[id=req], on.push|
|`CURRENT_FINAL_PLAN.md`|True|B01|R342-001||
|`app/build.gradle`|True|B01|R342-002|generateR334PlanIdentity, preBuild, tasks.configureEach, verifyFinalR334PlanIdentity|
|`app/src/androidTest/java/com/ethk3/novaonepro/r341/R341B08ContractTest.java`|True|B07|R342-026|test_r341_b08_04_a01|
|`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B02ContractTest.java`|False|B02|R342-003, R342-004, R342-005, R342-006|test_r342_003, test_r342_004, test_r342_005, test_r342_006|
|`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B03ContractTest.java`|False|B03|R342-007, R342-008, R342-009, R342-010, R342-011|test_r342_007, test_r342_008, test_r342_009, test_r342_010, test_r342_011|
|`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B04ContractTest.java`|False|B04|R342-012, R342-013, R342-014|test_r342_012, test_r342_013, test_r342_014|
|`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B05ContractTest.java`|False|B05|R342-015, R342-016, R342-017, R342-018, R342-019|test_r342_015, test_r342_016, test_r342_017, test_r342_018, test_r342_019|
|`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B06ContractTest.java`|False|B06|R342-020, R342-021, R342-022, R342-023|test_r342_020, test_r342_021, test_r342_022, test_r342_023|
|`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B07ContractTest.java`|False|B07|R342-024, R342-025, R342-026, R342-027, R342-028, R342-029, R342-030|test_r342_024, test_r342_025, test_r342_026, test_r342_027, test_r342_028, test_r342_029, test_r342_030|
|`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B08ContractTest.java`|False|B08|R342-031, R342-032, R342-033, R342-034, R342-035, R342-036, R342-037, R342-038, R342-039, R342-040, R342-041, R342-042|test_r342_031, test_r342_032, test_r342_033, test_r342_034, test_r342_035, test_r342_036, test_r342_037, test_r342_038, test_r342_039, test_r342_040, test_r342_041, test_r342_042|
|`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B09ContractTest.java`|False|B09|R342-043, R342-044|test_r342_043, test_r342_044|
|`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B10ContractTest.java`|False|B10|R342-045, R342-046, R342-047, R342-048|test_r342_045, test_r342_046, test_r342_047, test_r342_048|
|`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B11ContractTest.java`|False|B11|R342-049, R342-050, R342-051, R342-052, R342-053, R342-054, R342-055|test_r342_049, test_r342_050, test_r342_051, test_r342_052, test_r342_053, test_r342_054, test_r342_055|
|`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B12ContractTest.java`|False|B12|R342-056, R342-057|test_r342_056, test_r342_057|
|`app/src/main/java/com/ethk3/novaonepro/AlgorithmLifecycleReportR927.java`|True|B11|R342-051|capture, captureSnapshotCP4|
|`app/src/main/java/com/ethk3/novaonepro/ApiClient.java`|True|B05|R342-015|requestGet, requestGovernedLatest|
|`app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java`|True|B02, B03, B04, B05|R342-004, R342-005, R342-007, R342-008, R342-012, R342-015, R342-016, R342-018|beginSession, dispatchResearchCaptureR946, drainWriterFaultControlR334, endSession, ensureContinuousRuntimeR26, nextDelay, postResearchControlR31, prepareAnalysisHistoryR364, reconcilePredrawTailRecoveryR32, reconcileResearchOwner, run, runFetchChannelOnce, scheduleNextFetchLoop, snapshot, startWork, wake|
|`app/src/main/java/com/ethk3/novaonepro/BackgroundRuntimeContractR922.java`|True|B05|R342-018|evaluate|
|`app/src/main/java/com/ethk3/novaonepro/BuildIdentity.java`|True|B11|R342-049|verifyAndPersist|
|`app/src/main/java/com/ethk3/novaonepro/BusinessFunnelCP12.java`|True|B09|R342-044|capture, captureSnapshotCP12, readStatement, statementCP12|
|`app/src/main/java/com/ethk3/novaonepro/CandidateCalibrationR932.java`|True|B09|R342-043|captureOrPredict, fitModel, fitOne, inputStateCP12, loadFit, publishFit, qualificationForStudyCP12, settle|
|`app/src/main/java/com/ethk3/novaonepro/CanonicalObservationWriterR36.java`|True|B09|R342-044|fromRuntime, insertWithinTransaction|
|`app/src/main/java/com/ethk3/novaonepro/CertificateBundleServiceR929.java`|True|B09|R342-044|cp4ExternalGates, evaluateCp4, validatedCp4Cap, validatedInfluenceCap|
|`app/src/main/java/com/ethk3/novaonepro/CertificateServiceV2R929.java`|True|B11|R342-051|evaluate, permitsRankingInfluence|
|`app/src/main/java/com/ethk3/novaonepro/CertifiedPredictionR974.java`|True|B09|R342-043, R342-044|beforeInsert, certificateContract, evaluate, prepare, readExactCalibration, requireExactCalibrationObservations, requireExactCalibrationScopeGate, verifyCertificate|
|`app/src/main/java/com/ethk3/novaonepro/CheckpointReportAuditR32.java`|True|B10|R342-046|observe|
|`app/src/main/java/com/ethk3/novaonepro/CrashRecorder.java`|True|B05|R342-019|detailedSummary, persistOomLowAllocation|
|`app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java`|True|B02, B04, B05, B11|R342-003, R342-004, R342-005, R342-012, R342-014, R342-017, R342-049|commitOutcomeName, failureCategory, observeResearchCommitR341, operation, phase, runWrite, runWriteOrThrowF01, writeProtocolSnapshotR31|
|`app/src/main/java/com/ethk3/novaonepro/DbStore.java`|True|B04, B09|R342-012, R342-044|call, insertAuthorizedTradeWithinCanonicalR915, saveHistoryPageR32, savePredrawSuffixR32|
|`app/src/main/java/com/ethk3/novaonepro/DecisionGateEngine.java`|True|B09|R342-044|b12Reject, decideV51B12, decideV51B12OriginalR30, toLegacyDecisionV51|
|`app/src/main/java/com/ethk3/novaonepro/DeferredEvidenceSnapshotR36.java`|True|B05|R342-017|parseDoubles|
|`app/src/main/java/com/ethk3/novaonepro/DeferredEvidenceWriterR36.java`|True|B04|R342-014|call, claim, writeOneInternal|
|`app/src/main/java/com/ethk3/novaonepro/DomainRouterR929.java`|True|B09|R342-043|route, routeWithEvidenceR82|
|`app/src/main/java/com/ethk3/novaonepro/FixedResearchWindowR932.java`|True|B05, B08, B09|R342-017, R342-034, R342-043|closeOne, freezeChampion, freezePair, maintain, maintainReviewed|
|`app/src/main/java/com/ethk3/novaonepro/FixedWindowStatisticsR932.java`|True|B08|R342-034|evaluate, evaluateCp4|
|`app/src/main/java/com/ethk3/novaonepro/FrameMetricsR906.java`|True|B05|R342-017|buildSummary, doFrame|
|`app/src/main/java/com/ethk3/novaonepro/FrozenCandidateAdapterR958.java`|True|B08, B09|R342-033, R342-044|capture, commitDevelopmentFreezeCP8, load, prepareDevelopmentFreezeCP8, recipeLaneCP12, scoreRecipeCP8, settle|
|`app/src/main/java/com/ethk3/novaonepro/FrozenDriftPolicyR82.java`|True|B09|R342-043|observe, parse|
|`app/src/main/java/com/ethk3/novaonepro/K3FailureDiagnosisEngine.java`|True|B08, B11|R342-039, R342-052|diagnose, diagnoseSnapshotCP4, stateLayer, stateLayerSnapshotCP4|
|`app/src/main/java/com/ethk3/novaonepro/LifecycleSchemaR31.java`|True|B07, B11|R342-026, R342-049|Definition, advanceWithinMigration, definitions, requireDefinitions|
|`app/src/main/java/com/ethk3/novaonepro/LiveResearchDiagnosisOrchestrator.java`|True|B04|R342-013, R342-014|appendRuntimeItemR76, collectRuntime, persistedNumberSurfaceR80, runtimeBusinessFailedR76, runtimeFailureItemsR76|
|`app/src/main/java/com/ethk3/novaonepro/ModelBundleRuntimeR932.java`|True|B11|R342-052|forEvidence, forHistoricalEvidence|
|`app/src/main/java/com/ethk3/novaonepro/MultiHorizonProbeR80.java`|True|B08|R342-042|fit|
|`app/src/main/java/com/ethk3/novaonepro/ReportAttachmentClosureCP12.java`|True|B02, B12|R342-006, R342-057|capture|
|`app/src/main/java/com/ethk3/novaonepro/ReportCatalogCP4.java`|True|B11|R342-053, R342-054|hasCompleteResult, writeReadableSummary|
|`app/src/main/java/com/ethk3/novaonepro/ReportDiagnosticEvidenceR64.java`|True|B12|R342-057|write|
|`app/src/main/java/com/ethk3/novaonepro/ReportExportServiceCP4.java`|True|B05|R342-018|runtimeOperatingStateR31|
|`app/src/main/java/com/ethk3/novaonepro/ReportReadContextCP4.java`|True|B12|R342-057|compactDiagnosticR64, evidenceMemberR64|
|`app/src/main/java/com/ethk3/novaonepro/ReportSemanticVerifierR31.java`|True|B11|R342-053|verifyDelivery, writeReceipt|
|`app/src/main/java/com/ethk3/novaonepro/ResearchFeedbackCP12.java`|True|B08|R342-039|drainOne|
|`app/src/main/java/com/ethk3/novaonepro/ResearchGatePlanR967.java`|True|B09, B11|R342-044, R342-051|bindCp4, forWindow, installed, policyForGeneration|
|`app/src/main/java/com/ethk3/novaonepro/ResearchMainlineContributionAuditR922.java`|True|B09|R342-044|exactRealAttributionR34, insertWithinTransaction, validateExactWithinTransaction|
|`app/src/main/java/com/ethk3/novaonepro/ResearchPlatformStoreR929.java`|True|B09|R342-044|authorizeCurrent, recordContribution|
|`app/src/main/java/com/ethk3/novaonepro/ResearchPredrawCaptureR923.java`|True|B08|R342-042|captureTarget, captureTargetWithinR927, publish|
|`app/src/main/java/com/ethk3/novaonepro/RuntimeHealthLedgerR36.java`|True|B04, B05|R342-012, R342-013, R342-016|applyPerformanceR80, classifyBusinessR77, measuredDurableR80, readProgressR34|
|`app/src/main/java/com/ethk3/novaonepro/RuntimeTruthSnapshotR906.java`|True|B04|R342-013|pipelineDetail, pipelineDetailSnapshotCP4|
|`app/src/main/java/com/ethk3/novaonepro/ServiceHeartbeatStore.java`|True|B03|R342-008|getBusinessStaleReason|
|`app/src/main/java/com/ethk3/novaonepro/SettlementBacklogAuditR3611.java`|True|B04|R342-013|classify, isTerminal, scan|
|`app/src/main/java/com/ethk3/novaonepro/SettlementCoordinatorR815.java`|True|B09|R342-044|drain, execute, settleNow|
|`app/src/main/java/com/ethk3/novaonepro/SettlementEngine.java`|True|B09|R342-044|executeFromCoordinatorR815, settleRealWithinTransactionR36|
|`app/src/main/java/com/ethk3/novaonepro/StartupStageJournalR341.java`|True|B02, B03|R342-003, R342-007|classify, code, hasDeterministicFailureLimit, recordUncaught|
|`app/src/main/java/com/ethk3/novaonepro/StorageCensusSU2.java`|True|B10|R342-045, R342-047, R342-048|detail, protection, sqliteSpace|
|`app/src/main/java/com/ethk3/novaonepro/UnifiedResearchChoiceR932.java`|True|B09|R342-044|freeze, prepareChoiceCP12, selected|
|`app/src/main/java/com/ethk3/novaonepro/V51ActionRankerArtifact.java`|True|B11|R342-052|；字段：DATA_SHA256, PROVEN, RANK_ORDER_PASSED|
|`app/src/main/java/com/ethk3/novaonepro/V51ActionRankerEngine.java`|True|B11|R342-052|rankDetailed, rankOne|
|`app/src/main/java/com/ethk3/novaonepro/V51CandidateRealGateR915.java`|True|B09|R342-044|evaluate, releaseBoundary, verifyAuthorized|
|`app/src/main/java/com/ethk3/novaonepro/V51NumberModelArtifact.java`|True|B11|R342-052|；字段：DATA_SHA256, PARAMETER_SHA256, PROVEN|
|`app/src/main/java/com/ethk3/novaonepro/V51PermutationNumberModelEngine.java`|True|B11|R342-052|predict, productionModelProven|
|`app/src/main/java/com/ethk3/novaonepro/V51Repository.java`|True|B09|R342-044|commitCanonicalBundleR36, commitCanonicalBundleVerifiedR368, insertAuthorizationCoreR36|
|`app/src/main/java/com/ethk3/novaonepro/V51RuntimeOrchestrator.java`|True|B09|R342-044|buildRankedActionStage, finishPreparedR335, resumePreparedR335, runR36|
|`app/src/main/java/com/ethk3/novaonepro/V51SelectedActionCalibrationEngine.java`|True|B11|R342-052|calibrateDetailed, probabilityEvidence|
|`app/src/main/java/com/ethk3/novaonepro/r952/DevelopmentProductionCP12.java`|True|B06|R342-021, R342-022|commitFeaturePage, prepare, prepareBatch|
|`app/src/main/java/com/ethk3/novaonepro/r952/DevelopmentTrainerCP9.java`|True|B06|R342-022|checkpoint, finish, restore, step|
|`app/src/main/java/com/ethk3/novaonepro/r952/EvolutionControllerR952.java`|True|B07, B08|R342-025, R342-039, R342-040, R342-041|claimEvolution, drainActionCP12|
|`app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java`|True|B03, B06, B07|R342-011, R342-020, R342-025|guardComplete, holdCursor, maintain, prepare, register|
|`app/src/main/java/com/ethk3/novaonepro/r952/ModernSeedFactoryCP12.java`|True|B06, B07|R342-020, R342-029|completeCP12, maintain|
|`app/src/main/java/com/ethk3/novaonepro/r952/ResearchDirectorR952.java`|True|B03, B06, B07, B08, B09|R342-010, R342-011, R342-020, R342-022, R342-027, R342-028, R342-032, R342-033, R342-036, R342-037, R342-044|bootstrap, capturePredraw, commitCandidateBatchCP11, commitPreparedCandidateCP5, commitTrainingLabel, drainImportSliceCP12, onSettlementCompleted, prepareTrainingLabel, pulse, runDirectorSlotR65|
|`app/src/main/java/com/ethk3/novaonepro/r952/ResearchTrainingHookRegistryR952.java`|True|B06|R342-022|drainTrainingKindR65, drainTrainingSlotCP11, prepareCandidateBatchCP11|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetPublicationR31.java`|True|B03|R342-009|schemaObjectsR31|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetRuntimeR30.java`|True|B03, B08|R342-009, R342-031||
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomPublicationStoreR31.java`|True|B07|R342-026|declaresRuleObjectR342, open, prepare, prepareRuleR342, publish, publishRuleR342, readRulesR342, requireReadyR31, ruleDdlR342|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomWorldR27.java`|True|B07|R342-026|atomsStep, derived, inductGenerationRulesR342, molecules|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/CertificateGateR27.java`|True|B09, B11|R342-044, R342-051|choose, contract, evaluate|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/CertificateSU1.java`|True|B09|R342-044|controlsComplete, maintain, metrics, read|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/CertificateSU2.java`|True|B09, B11|R342-044, R342-051|maintain, maintainOne, maintainPolicy, preregister, read|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/ColdRebirthR34.java`|True|B07|R342-024|step|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/ColdRebirthSchemaR34.java`|True|B03|R342-009|declares, require|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/EvolutionLoopR27.java`|True|B07, B08|R342-026, R342-040|commitResearchAdmissions, stageResearchHypotheses, stepBirth, stepGate|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/FailureDispositionR31.java`|True|B02|R342-003|classify, commitOutcomeName, consumesFaultBudget, declaredCategoryR31, failureCategory, firstTypedR31, operation, ownCategoryR31, phase, physicalCauseCategoryR31, rootCategoryR31|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyRegistryR26.java`|True|B07|R342-030|bindLegacyTeacher, build|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/FieldFollowupR34.java`|True|B03, B11|R342-009, R342-049|capture|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/FoundryCampaignR26.java`|True|B06, B07, B08|R342-023, R342-024, R342-025, R342-040|step|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/FrozenConfirmationR30.java`|True|B08|R342-034|evaluate, freeze|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/FrozenConfirmationStoreR30.java`|True|B08|R342-034|complete, freeze, register|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/FusionOpsR27.java`|True|B08|R342-041|fuse|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/FusionStudyR30R1.java`|True|B08|R342-041|gain, pair|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/IdeaFamilyR27.java`|True|B07|R342-026|generate, propose|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineAdapterR31.java`|True|B09|R342-044|prepare, verifyFrozen|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineAdapterSU1.java`|True|B09|R342-044|authority, bind, first, prepare, verifyFrozen|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineAdapterSU2.java`|True|B09|R342-044|prepare|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineFreezeProofR31.java`|True|B09|R342-044|allows, certificate, freeze, publishWithinFreeze, read|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixResearchR30R1.java`|True|B08|R342-035|dependencyKeyR31, produceR31, step|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixTensorR30R1.java`|True|B08|R342-035|append|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/RealAuthoritySU2.java`|True|B09|R342-044|allows, completeFreezeAndNoRealR30|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/RepairEffectLedgerR341.java`|True|B11|R342-049|appendObservation, installWithin, issueFor, read|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportAssessmentR33.java`|True|B11|R342-053, R342-054|evaluate, failed, finite, group, map, verifiedPass|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportSnapshotR30.java`|True|B11|R342-055|add, capture, count, has, rows, sql|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchActionPolicyR33.java`|True|B08|R342-039|Source.requireAction, weights|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchActionStoreR33.java`|True|B08|R342-039|append|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchConsumptionStoreR31.java`|True|B07, B08|R342-026, R342-039|ablation, completeRuleUseWithinR342, completeWithin, report|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchContextR30R1.java`|True|B07, B08|R342-026, R342-035, R342-039|appendBudgetReceipt, proposeFamilyR31, proposeRuleR342, read, readRuleSnapshotR342, stageBoundR31, stageRuleTransferR342|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchInputPinR30R1.java`|True|B06, B08|R342-023, R342-038|appendAdmitted, loadCurrent, require, revision, runValid|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchObservatoryR30R1.java`|True|B07, B08, B09, B11, B12|R342-028, R342-029, R342-031, R342-034, R342-038, R342-041, R342-044, R342-050, R342-056|classifyFrameR335, compute, productR335, step, stored|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchRetentionR30.java`|True|B10|R342-045, R342-047|clean, kind|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchRetentionStoreR30.java`|True|B03, B10|R342-007, R342-045, R342-046|afterDelete, beforeDelete, cleanFiles, preservePreparedStartupCheckpoint, preserveStartupCheckpoint, retiredCopyReference|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchSourceJournalR31.java`|True|B08|R342-038|DatasetRead.capture, affectedSql|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchStoreR30R1.java`|True|B07, B08|R342-026, R342-038, R342-039|consume, currentSource, paired, ready|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchStudyTaskR30R1.java`|True|B08|R342-038|step|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeCoordinatorR26.java`|True|B03|R342-007, R342-008, R342-009|activeHealthObservationR31, initialize, isRunning, prepareStartupSchemaR34|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeR27.java`|True|B06, B07, B08|R342-023, R342-026, R342-035, R342-038, R342-039|AtomsTask.step, DatasetTask.step, EvolutionTask.step, MatrixProductionTask.step, ObservatoryTask.step, ResearchStudyTask.step|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/StartupCheckpointIdentityR31.java`|True|B10|R342-046|prepare|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/Su2CommitAuthorityR30.java`|True|B09|R342-044|verifyFrozen|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/TypedGenerationRuleR342.java`|False|B07|R342-026|decode, encodeCanonical, induct, instantiate, ruleId, shapeHash, validate|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/WriterHoldFaultsR31.java`|True|B02, B11|R342-004, R342-005, R342-006, R342-055|deferResearch, drainBatchR33, drainControlSliceR32, drainOne, snapshot|
|`construction/plans/STARCORE_R34_1_REPAIR_20261124/MANIFEST_SHA256.json`|True|B01|R342-001||
|`construction/plans/STARCORE_R34_2_REPAIR_20261125/FILE_CARDS.json`|False|B01|R342-001||
|`construction/plans/STARCORE_R34_2_REPAIR_20261125/control/PLAN_IDENTITY.json`|False|B01|R342-001||
|`release/r342_plan_binding.json`|False|B01|R342-002|$.planCommit, $.planSha256, $.series, $.sourceApkEvidenceRef, $.sourceBaseline|
|`tools/r33/advance_release_version.py`|True|B01|R342-002|apply, apply_locked, main, prepare, repo_version_lock|
|`tools/r33/package_ci_candidate.py`|True|B01|R342-002|embedded, main, validate_pair|
|`tools/r33/run_release_build.py`|True|B01|R342-002|build_environment, main|
|`tools/r33/verify_plan_binding.py`|True|B01|R342-002|identity, main, release_owner|
|`tools/r341/publish_delivery_manifest.py`|True|B01|R342-002|collect, main|
|`tools/r342/build_identity.py`|False|B01|R342-002|identity, verify_seal|
|`tools/r342/test_b01_contracts.py`|False|B01|R342-001, R342-002|test_r342_001, test_r342_002|
