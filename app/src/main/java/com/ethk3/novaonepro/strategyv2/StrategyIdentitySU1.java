package com.ethk3.novaonepro.strategyv2;

/** A new structure/model/update/selector/calibration/environment always opens new evidence. */
public final class StrategyIdentitySU1 {
    public final String hash;
    public StrategyIdentitySU1(String program,String training,String weights,String update,String selector,String calibration,String environment) {
        hash=HashingSU1.fields("SU1_IDENTITY_V1",HashingSU1.sha(program),HashingSU1.sha(training),HashingSU1.sha(weights),
                HashingSU1.sha(update),HashingSU1.sha(selector),HashingSU1.sha(calibration),HashingSU1.sha(environment));
    }
}
