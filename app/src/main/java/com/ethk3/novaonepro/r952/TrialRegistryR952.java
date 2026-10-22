package com.ethk3.novaonepro.r952;

import com.ethk3.novaonepro.DbStore;
import com.ethk3.novaonepro.ExperimentProtocolR929;
import com.ethk3.novaonepro.ResearchPlatformStoreR929;

/** Adapter to the existing unique trial owner; no duplicate trial table or sealed-data budget. */
public final class TrialRegistryR952 {private TrialRegistryR952(){}public static boolean register(DbStore store,ExperimentProtocolR929.TrialRecord trial){return ResearchPlatformStoreR929.registerTrial(store,trial);}}
