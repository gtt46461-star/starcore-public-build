package com.ethk3.novaonepro;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Build-time SHA-256 manifest for every source owner protected by the R915 Legacy fixed-asset
 * contract. The release gate recomputes these hashes from the delivered source tree; this class is
 * also the exact runtime source-lock baseline persisted to SQLite.
 */
public final class LegacySourceLockManifestR915 {
    private static final Map<String,String> LOCKS = build();

    private LegacySourceLockManifestR915() {}

    public static String digest(String owner) { return LOCKS.get(owner); }
    public static int size() { return LOCKS.size(); }
    public static Map<String,String> all() { return LOCKS; }

    public static String fingerprint() {
        StringBuilder out = new StringBuilder("R915_LEGACY_SOURCE_LOCKS");
        for (Map.Entry<String,String> row : LOCKS.entrySet())
            out.append('|').append(row.getKey()).append('=').append(row.getValue());
        return V51ChecksumEngine.sha256(out.toString());
    }

    public static boolean isSha256(String value) {
        if (value == null || value.length() != 64) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = Character.toLowerCase(value.charAt(i));
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) return false;
        }
        return true;
    }

    private static Map<String,String> build() {
        LinkedHashMap<String,String> out = new LinkedHashMap<String,String>();
        lock(out,"ActionUtilityEngine","3339c370d6aa22c846e2a2a0cc8fee0a4401d571cdbcef49b2255b7b598f421d");
        lock(out,"AdaptiveExpertConsensusEngine","aaee47d886687ce65bb08f6536d27e266125715d4b77b34fa2aec1a22141b0bf");
        lock(out,"AdaptiveLearningJudgeEngine","a0a18a61427ab96f8ca0fbf047b4b0407d4abc0c198389748c6f6bd39d30fdb0");
        lock(out,"AiEnsembleJudgeEngine","7c2853e9b5be5f3f5b4da1e06a0cbae77d61c33e20325335ee58a852ba20e548");
        lock(out,"AssociationGraphEngine","210937d7b1ab720b0820be570b9fd906b406175b201b38ec08c7ec102a7779ef");
        lock(out,"AttributionEngine","b31783f337dd73ff62e26081a4905409c66e3f43c9e3e3b425a576df2e0ebcf4");
        lock(out,"BayesianEvidenceFusionEngine","2bff1fe5861248ca3c1d253cf264a2d885a47234fafc279c22b2c169ef6dff7a");
        lock(out,"BiasEvidenceEngine","f7da256b745916ba28fd08d0f0df9bdb68d86fa56a2ef8b9841d86f644daa8b0");
        lock(out,"BreakRecoveryEngine","8a37c55ec43103706e0c4db4cb67c8a8352e8ba14da8f5055cd52055792224a0");
        lock(out,"CalibrationEngine","081a15f25293959d135345ae5f273599310b8739c8af1b67bc2f5c4a5cd19b7d");
        lock(out,"CandidateEvidenceEngine","0d10677afa397883b3a95d47f8ba59201d98323d23e8ab8b06e385c83d400843");
        lock(out,"CausalFactorMatrixEngine","f0cc278953212620cba8252e29bf34189eb7f97359258697208b4bd437977bae");
        lock(out,"ComboScannerEngine","08ecb0302681e4b59e4f09a149e08acd887f93bc36c7fa447e0491298afa4322");
        lock(out,"ContrastiveReasonEngine","52cd32a31f33ba209bf24b806b6af1d19771a36a3fec1cac3407a27203f2a869");
        lock(out,"CounterfactualDistillationEngine","e20dc0909d858007e7c7f442d3f5b1fa757ea054c2e7d8b2ef596dbcb32e1004");
        lock(out,"CounterfactualRankEngine","749a0fe494a6b8fdd1a5704c02db5884d86623d8e35d66f179fdfbcd6c456a6c");
        lock(out,"CounterfactualReasonEngine","afd7951e0b25a59cd4b22a30e4469b7fcfa9c81972567ebd66acb8ec46ccad2f");
        lock(out,"CrossPositionAssignmentResearchEngine","f01df6ecdbf0d56dbb84fe249713725fffeaf73a02980650a6338c5969ce3eb4");
        lock(out,"CycleRhythmEngine","8aba4d3be6f057582fed5a79132ec7777cf733782cff02dbbbd80b0d8c9cad31");
        lock(out,"DecisionAuthorizationEngine","eb805a0e5cae2d5703291c667133a182c9056bc5f497c8c4033eb1e145af7dc9");
        lock(out,"DecisionFunnelDiagnosisEngine","83384e3900aceb2005357be63af417dcc8667dc2993cf2d6ecaf3c9c501fb902");
        lock(out,"DecisionGateEngine","428455f117549f91cf4d70f0531bd3962b088398b93fb2fa6ef5a6e89d608144");
        lock(out,"DeepFeatureEngine","036f4cd14c6773a453446dfae7a007d618cc9e0cee4f7c388104acf13667acec");
        lock(out,"DeepLearningBridgeEngine","397aba6ceaef6e33b665364b5b35677eef21302528498f9759844b462243d81c");
        lock(out,"DeepProfileEngine","d7144fc487102c25b482f85e76d334ed658dd8ea816b2a21cb56c816822f0c91");
        lock(out,"DiagnosisFixPlanEngine","f69708565ce92bbc2a73a826e547c37b197b054aeefae86a959c7fbdd6402b71");
        lock(out,"DistributionDriftEngine","52eb273b1f8219e52f178484fc6069392b5035f98ca8129e9aa51edc45dfabaf");
        lock(out,"DynamicCauseChainEngine","d8ae64cbf41154873324347cb123dae737530b2be1e633d75564cb35cf95d3fb");
        lock(out,"DynamicFreezeMatrixEngine","1fab2d5847fe8ef57ddfd0bfdc171db6b80b48eafc344c783c278460660b3bd9");
        lock(out,"DynamicWindowClusterEngine","7a3944c1f426bf6ebb5cb1b388d3de54da4b0a5a037e0f7683466fb00b856ce6");
        lock(out,"EvidenceHealthSnapshotEngine","aeebdc3652d8958b20ef6fb67f3d308f1d847a435557e3462a6a21819e172807");
        lock(out,"EvidenceRankerEngine","2c5b0825e3f156d375ffe7f2b938801b0c9fe18eb04a6abfc0bbf849ffb0cce7");
        lock(out,"FalseStrongFreezeEngine","25cc68228f6ea5740686d4f3b0ea46c5cd4921bdf86add03d265f53404eed23d");
        lock(out,"FullCandidateScanEngine","5a29b1f01d4b1f669f9a1000cdf139dc635203d1fe4d095c68c4f7d22673eecc");
        lock(out,"FullInformationFeatureGraphEngine","1fa2237fd186db0ca8fd5b832031428cc6cc52dfb8c170a54e3dcde8ddc355cf");
        lock(out,"HierarchicalPosteriorEngine","5d819a9ca5654fb107e006c6d0be577d3397482b605251dd83035e46f53db23f");
        lock(out,"HierarchicalStateTaxonomyEngine","d21788f1ccae5236fe7a45c988c9f14421e716e3bf03a67224f4da8e1ea7598f");
        lock(out,"HistoricalStatePriorEngine","4da526dc6c331494a6e5fbdb2c49deec23bb5e3bd606ab6ccca44b8c18934cc7");
        lock(out,"K3ActionSpaceEngine","25925852af63bed57b010e30051fbacee9d150c98c6b11de2ea7fb2e5295ec85");
        lock(out,"K3RoleComboEngine","b3255874f834c797fb53559dd9bcc2414da6f3396eeb72df48fe94ea15f888d5");
        lock(out,"LanePolicyCompiler","2b6459c31926955fa41c1ece0ce3fe15d70d8f314112d0c3e0b7fb01088820b3");
        lock(out,"LearningEngine","1ce299e263e8c613f68e72ec75f93f1bad17193cc6a5299a37efffbc94631ed1");
        lock(out,"ManualDeepBacktestEngine","01b05f9e87ed11b89c187075fa07a9f2191f24b2aad94d6a500fc66a36c66be0");
        lock(out,"MarkovEngine","02a98cf9df974ff45ac2787b5553963c96e12f781778e23958e567251b429cd4");
        lock(out,"MicroWalkForwardEngine","45905767f37c55b797abc9b75594ec9c6ad6d7477ceea90621877d00dc909b92");
        lock(out,"ModuleEffectScoreEngine","6b114b06b1b0c218d586be2e1b9f40831916d3eb26de5709ded734daea76c543");
        lock(out,"MonotonicCalibrationEngine","710c0acf54bf7fd0cffb735d5bacb01b8a474df850a357118d5b55239cf94e59");
        lock(out,"NeedSupplyReasonEngine","7c1c1e7ecc72ce7fbcc436316bcca9dfd1a3ee0ec380995d64ae56f90684a71b");
        lock(out,"NumberBehaviorEngine","df036d6dc267fbe0d29ac94854c1a01947b3cc9d513f3d75ffbc574c20c89e85");
        lock(out,"NumberSurfaceTensorEngine","e339879f762bdae5074c772996b9dbf0bd7c3679bafce1ee2b14602212e65ddb");
        lock(out,"OnlineActionModelEngine","63a3ea34ac8e7886e46ba4635f95780bc1bb4729a8063ad42968a1658d7689d4");
        lock(out,"OnlineEvidenceForestEngine","2452ec03445c9065c0f8bf1bdf2eb49b91da9413492d0d1e0b9f7d0b723164d0");
        lock(out,"OosActionLogEngine","2cc45ba369afab67c247d3511ae66865567c9d5c6c8a6cb56a5cf6a8b49a8eed");
        lock(out,"OosEligibilityEngine","fc4829c0951bdb99e8bd8f4e4be5dcfce9b1ee46dd76c118f89db145bcd27dd7");
        lock(out,"OosReliabilityAuditEngine","975832021e63f4910baf030074aa78c01d5703927b503b44112cdb4e7e4ae0b9");
        lock(out,"OosTrainingCoordinatorEngine","714807c227a36a1ee4b7b554127364903b156c598174f31234f7e176b9d04add");
        lock(out,"OpportunityMinerEngine","209bae47e307fa9d3566e18c8d2e08e69a2d9c0f1cc1d66a4f8a58af1ace203b");
        lock(out,"PatternStateEngine","d609761eba576bb88717ae93340d95f27a140f1e4fbe59adf91127cc4eca1eb9");
        lock(out,"PointInTimeNumberFeatureEngine","fc635a3f564634a3b39f9c186b45c0600e7c812f63f7855b08923c853762e91c");
        lock(out,"PointInTimeSnapshotEngine","c2cb0de9336596e5ea16ee37c30b06eb7ba28abe1a6bf6736ed479b54145ff98");
        lock(out,"PositionCauseMatrixEngine","d3ba06b5b7d9b5e72bc191375e759057b2cd6378086b0bdb21e0e2603438903c");
        lock(out,"PositionStrengthEngine","63e65a92b48ebce78ed0ba972a3bbc096567f0e663555503a0db8b59fedc15e4");
        lock(out,"PreDrawFeatureEngine","c70c6b007a9fd5072efe08b98f6e0a85ed6278deeb2329345a6c9bc9bd61bb46");
        lock(out,"PreDrawReasonEngine","24354781c9f8427e93b2cb93eaf43272fc4f11816a3f69b0f0e55fc9d8b94b35");
        lock(out,"ProbabilityAuditEngine","f0bcc36792a8e83e7886eea5e2e01c3dbfca9eaa97115677618f6cf46caf946d");
        lock(out,"ProbabilityEngine","c9e2544db0c14bc82e4ce59f025d39f567abca86577d841684b6a62f059ab200");
        lock(out,"ProfileFallbackEngine","ab1484e27304f4a14a38c1461872650324c8f237e03ff9632728edc056e61ff8");
        lock(out,"PromotionGateEngine","f8bd5a795cc72798f442f822d8b787beb96499ccad22f7bea4c9e3bfe26a7ba7");
        lock(out,"RealBetDiagnosisEngine","459dab7540c8cc5b23e43428c9b61387f09f3b536c0eeaa0992179ba45619602");
        lock(out,"RealtimeCausalProbabilityEngine","fc754d0bd5e023b082b8f3075735a1058745b5c75ecf6a32c2e1edc8edd0edc7");
        lock(out,"ReasonCausalMatrixEngine","8207f98d3e76c9fc78ebe0f0574c30d00179a94990ad3f4e989b2dfc4172bc42");
        lock(out,"ReasonEngine","7207d579135f04eae7b886e526bb1e527fb8d2a30755447ebd1bf629457f5138");
        lock(out,"RegimeRegistryEngine","1e41334b1dd6c376f6a44877a7e2814afbaca2c37591e101428ffad0eac425c5");
        lock(out,"RelationTensorEngine","397f8157007e9d729df8e2c162263186a87cf9891dbc07396d7562362ca4591f");
        lock(out,"RiskCoverageController","deecf89d5beda2c090041cfd63ab8d6f83706d20b999f101b818ed2638ee9e9b");
        lock(out,"RollingCausalBacktestEngine","e041a87f07e1cc2cfd075e0f33cd48d194074de4c6b2efa0a3c9364d58b7ef09");
        lock(out,"RuleHealthMatrixEngine","3a6f0dbfea7ba91b18ec0668998117bfa73ab9b20211407f344b35ccb1163669");
        lock(out,"SelfDiagnosisEngine","44532931bacc9968178ad7a03aa57c5465508e98fa9d0cbe48478de606dd1c8b");
        lock(out,"SequenceDistributionEngine","ffc10d0cafdbbb3cb0237b668495befef94f3504f16445b9eb8ab7bac11e1488");
        lock(out,"SettlementEngine","0dde0c59781661a17285ea78d08bfd6993d832ab3c1ca39126b8fdd6f41645a9");
        lock(out,"ShadowHealthGateEngine","f9b07b3ebea0fbd6b237354c636a504d794b70811de613e866b21530066def38");
        lock(out,"ShadowPromotionVerifierEngine","7791912d9f1914bf1bf76efe4b51bfc5c4dc8559bd6d7b736f561c8022e827b8");
        lock(out,"ShapeMemoryEngine","7aa69c8a15d84513052aa313d58693bcef0f71c846e6026da26c7a5a411f9179");
        lock(out,"ShapeStateEngine","6f42f2dab2c45d9aa7c0417e56d1fccd5592f8c86d655255b67cbc2e9d554b12");
        lock(out,"SimilarShapeEngine","8495789ca8b5fe13cf4a524d21ec4667a3a3bf3b39e7f72074e53543ed9ea293");
        lock(out,"TeacherMatrixEngine","45aa7a6003316897522169f8db9ea0f3efbdc0163d3126f720fde849594a0023");
        lock(out,"TeacherNumberMatrixEngine","17f4073f3e7ad0e2ffc4160795b2baa25e743d78fe3ef70bf66b9524e27ae15f");
        lock(out,"ThirdGuardMatrixEngine","d729216dcce461b8d6b6bee2b7e6c9db59ff7bac6637122916ba0ed1d7b4f07f");
        lock(out,"TransitionRelationEngine","4dc8ddd96e71fd2fa86e187d658c1260d27d75715215ae3c97df2ec2f4540dbf");
        lock(out,"TrendStateMachine","3799e04f25987d131ada5990f5463c14a0a09252d80d7bc35d5c576359bde518");
        lock(out,"UnifiedStrategyEvidenceEngine","5909ab62057a7fd46d1b817f007236027442c0dc391f47f13fd7bd63845a5f39");
        lock(out,"V51ActionMetaFeatureEngine","54c7d4797198e2c748de384831cafcd356c17b3f52306bcda8799ffd5da1883c");
        lock(out,"V51ActionRankerEngine","12dff3eebe9f6d91140186bfd2092476663d40bf24fcaf3dfbe105eae5feb904");
        lock(out,"V51ChangePointEngine","b10f89846a4b176e9b55770b45c73f569f055ef37c3b58ca0006a39b42f4531a");
        lock(out,"V51ContextTreeNumberExpert","8a5a37f8e082c39d55b86857567de47b03648e6565a8b5a2c7231a260cdbb69c");
        lock(out,"V51CrossBlockDomainCertificateEngine","e344bc7a32689d34cce98e95a8ffb5e6f53f60c5b1ee8950371cf019deadf80b");
        lock(out,"V51DomainCertificateEngine","c1b1f3af126ce363c44c0d3b61e8dd60ac54b966461a2ebc82d7fbb761bd446a");
        lock(out,"V51EffectiveConsensusEngine","c377b5791805aca2a8784604ca76054498a12a716c033be8978dfe59c1755144");
        lock(out,"V51ExpertGateEngine","1b4b4cf3fe899e10c98359b42ebbffc7d8bc78740411bc0a8a15f11a232d6e62");
        lock(out,"V51ExpertRegretEngine","bb07d9d94fc5627791d9d741d3d3e4445e42624fe56401157f21bfb7aa0b3075");
        lock(out,"V51FalsifierExpert","03f05505b5842519464356229a5586ea8842ad3c16d7dd6e7a3d3eafa85b868f");
        lock(out,"V51FeatureContractRegistry","434c6ec522e5833e5e6a43f15679e322ba1e4712c023e75e75ec25ccbe3a98be");
        lock(out,"V51FieldOwnerRegistry","1350c5f8435aa786f4864e80bf4483bec7225fb6176dc278768b4c353e78ab38");
        lock(out,"V51HierarchicalPriorNumberExpert","25d92e20452bbe31804caccc1bb734dd05ef6b827ffc10c5d937f980506acf2b");
        lock(out,"V51LifecycleEngine","f2a0e141324149290ead4be351addf0ff9a629f32c0e903a9dc9da51f1bf1fdc");
        lock(out,"V51MotifMatrixProfileEngine","139e5804a9168fba66151808075a5bf29b93ca5ff2528f655e4287a0a5f65aa5");
        lock(out,"V51MotifNumberExpert","7d58edb14064b0094ee80aa2b3c1933de5f83c5490ec8c5ed636deba47fcb3b6");
        lock(out,"V51MultiScaleTrendNumberExpert","dfc9ecb70de932051f6e9b62919aa4fe3029e940e113a69b28950de87fa66f88");
        lock(out,"V51ObservableStateVectorEngine","1b4277c2d6e0b8175c0097221b5b0c0a44f873ea712422d7d98c77ee4b1a5cb7");
        lock(out,"V51OfflineResidualNumberExpert","a32533bc317c0ba1db86411a3d5cd26d521dcaccd558da26d90000da187a77fd");
        lock(out,"V51OnlineResidualNumberExpert","85c0feb9ec63a3f46ca92c0f565e9c57e3abb3462ae8a0d319daf7ae0e5c4853");
        lock(out,"V51PermutationFeatureEngine","99c3c310016368a1375335dd8d247bfdccebf8cc3e56c509200675dc3c22d182");
        lock(out,"V51PermutationNumberModelEngine","d1d3302565c881c3d23ee0c8866faa9c7d03032b80125227aa2d66c5c9a040fb");
        lock(out,"V51PolicySurfaceEngine","918a9fd449784849d643de92f0845f140ce556896b9d7cef5a61249bd2b49745");
        lock(out,"V51PositionActionBuilder","5a8528e9c5492230cb2b057da904d83ef1d151df55a21b0fa5f6b360312096a5");
        lock(out,"V51PredictabilityEngine","2a2521a765d22315fc77829b3deb25ead3c0599fa782a9248265e63f700dbe64");
        lock(out,"V51RankStabilityEngine","85971a62dbfd85e8f8611f123f629557ffe3c8e4a66fc3a5791ab03c4aa0d5a8");
        lock(out,"V51RawScoreModelEngine","026e65a858e79e57c9137f0519101ee4458bf364d3ed371d7cc63b9341c7da00");
        lock(out,"V51RelationGraphNumberExpert","29bc5ce6a0d312e062ed17f03ffeacb2912f0b5e6718ed08dce26cfa7e8af9dc");
        lock(out,"V51SelectedActionCalibrationEngine","488da5257f76020a1c5db3063965afcb43a8b255e9c67f95e723587ee59caf00");
        lock(out,"V51SelectiveRiskController","89697ce97dbcbcaa707f805fd62e60f0460e6810e8c12f3316cd6fa50d0067fc");
        lock(out,"V51SinkhornEngine","58c3810a7a1365a0e8054ed85bbf3ff529ecdb18f8d11169724c924427f604d2");
        lock(out,"V51StateDurationNumberExpert","e9df8e1d9b96ca40e6613c627a44056810b50cb7b9fb070e6156cfe5ab64639f");
        lock(out,"V51StatePrototypeEngine","bdacf6df2978334b98c3c6b0e7c7cb54ef27666fc5a7a72f3f872ad901aec9c9");
        lock(out,"V51StateTrajectoryEngine","8e5eba367acfbd083d595c1cb1207289a1f165583d79127c75af3a4c925374d8");
        lock(out,"V51TeacherPosteriorNumberExpert","c82d08167d383bc056814ea96c4477397b8465ad3878588670e69bf9cd0141f5");
        lock(out,"V51TianshuEvidenceBridge","9e342b49dee555f0bc4bcd44600f4b18c4aaa288b2f92cf040c1d88558cbe04b");
        lock(out,"V51TrajectoryFeatureEngine","04d338135b10d13e6bc39bf2e9e5f6ac678e3f19e4593a2d2cf0de91bc3390b5");
        lock(out,"V51TransitionTensorEngine","f5e0b288386219ee881192cb68be46120f285d861e8d1c2ac144f5a9bd142b43");
        lock(out,"V51UncertaintyEngine","6b8619caa18521991d3acf6e971ba99753a9acd70e8be82f6703017306820d73");
        lock(out,"V51WindowPosteriorEngine","2b379156dd5ef56e73d63eba21e920e7590a56d96d5820013e19c59aae7027a4");
        lock(out,"WalkForwardBacktestEngine","87aeee028f117ec88b4ee8d7b3a7d25a0f06fdddf0adc51937de931c6479f8ba");
        lock(out,"WindowFeatureEngine","18449ed6e608d6b4ac2f2f2dde77df0bb9a4b38a3e5515fe287079bd3fbf8833");
        return Collections.unmodifiableMap(out);
    }

    private static void lock(Map<String,String> rows, String owner, String digest) {
        if (!isSha256(digest)) throw new IllegalArgumentException("R915_INVALID_SOURCE_DIGEST:" + owner);
        if (rows.put(owner, digest.toLowerCase(Locale.US)) != null)
            throw new IllegalStateException("R915_DUPLICATE_SOURCE_LOCK:" + owner);
    }
}

