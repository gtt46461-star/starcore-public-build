package com.ethk3.novaonepro.r952;

import com.ethk3.novaonepro.DbStore;

/** Minimal module contract. Modules are research-only and can never authorize or create REAL. */
public interface ResearchModuleContractR952 {
    ResearchModuleDescriptorR952 descriptor();
    void install(DbStore store);
}
