package com.ethk3.novaonepro.r952;

import com.ethk3.novaonepro.V51ChecksumEngine;

public final class ResearchModuleDescriptorR952 {
    public final String id, revision, owner, contractHash;
    public final ResearchExtensionPointR952 point;
    public final boolean mayCreateReal;

    public ResearchModuleDescriptorR952(String moduleId, String moduleRevision,
            ResearchExtensionPointR952 extensionPoint, String moduleOwner) {
        id = require(moduleId, "MODULE_ID");
        revision = require(moduleRevision, "MODULE_REVISION");
        point = extensionPoint == null ? ResearchExtensionPointR952.REPORT : extensionPoint;
        owner = require(moduleOwner, "MODULE_OWNER");
        mayCreateReal = false;
        contractHash = V51ChecksumEngine.sha256(
                "R952_MODULE|" + id + '|' + revision + '|' + point.name() + '|' + owner + "|REAL=0");
    }

    private static String require(String value, String code) {
        String out = value == null ? "" : value.trim();
        if (out.length() == 0) throw new IllegalArgumentException("R952_" + code);
        return out;
    }
}
