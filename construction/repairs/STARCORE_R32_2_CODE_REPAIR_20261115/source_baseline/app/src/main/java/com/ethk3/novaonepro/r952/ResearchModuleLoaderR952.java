package com.ethk3.novaonepro.r952;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Deterministic built-in loader. Asset JSON is a sealed audit mirror, never executable code. */
public final class ResearchModuleLoaderR952 {
    private ResearchModuleLoaderR952() {}

    public static List<ResearchModuleDescriptorR952> builtIns() {
        ArrayList<ResearchModuleDescriptorR952> out = new ArrayList<ResearchModuleDescriptorR952>();
        out.add(new ResearchModuleDescriptorR952("runtime", "1", ResearchExtensionPointR952.SETTLEMENT, "SettlementEngine_ONLY"));
        out.add(new ResearchModuleDescriptorR952("pit", "1", ResearchExtensionPointR952.PREDRAW, "ResearchDirectorR952"));
        out.add(new ResearchModuleDescriptorR952("world", "1", ResearchExtensionPointR952.LIFECYCLE, "WorldModelRepositoryR952"));
        out.add(new ResearchModuleDescriptorR952("model", "1", ResearchExtensionPointR952.TRAINING, "ResearchTrainingHookRegistryR952"));
        out.add(new ResearchModuleDescriptorR952("certificate", "1", ResearchExtensionPointR952.LIFECYCLE, "ResearchPlatformStoreR929"));
        out.add(new ResearchModuleDescriptorR952("causal_matrix", "1", ResearchExtensionPointR952.LIFECYCLE, "MatrixSnapshotBusR952"));
        out.add(new ResearchModuleDescriptorR952("research_creation", "1", ResearchExtensionPointR952.PREDRAW, "ResearchDirectorR952"));
        out.add(new ResearchModuleDescriptorR952("evolution", "1", ResearchExtensionPointR952.LIFECYCLE, "PostSettlementResearchLoopR952"));
        out.add(new ResearchModuleDescriptorR952("ui", "1", ResearchExtensionPointR952.PAGE, "FullbloodPageProviderR952"));
        return Collections.unmodifiableList(out);
    }
}
