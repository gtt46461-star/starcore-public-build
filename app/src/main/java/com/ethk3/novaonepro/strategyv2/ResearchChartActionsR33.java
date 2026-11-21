package com.ethk3.novaonepro.strategyv2;

/** Research actions shown by the diagnostic report. Kept separate from qualification authority. */
final class ResearchChartActionsR33 {
    private ResearchChartActionsR33() {}

    static String action(String view) {
        switch (view) {
            case "R01": return "按冻结窗口读结构状态与下一期目标，比较固定窗口与移除状态输入的子代，保留所选窗口和配对差。";
            case "R02": return "追回该期赛前结构概率，按相同目标绑定开奖误差，将失败簇交给FailureProfile，缺冻结时列未观察期。";
            case "R03": return "查九个同伴位置的事前输入和掩码，生成跨位对照与消融，登记同一目标的子代变化。";
            case "R04": return "回读各位置候选集合、交集及支持数，保持单位置K3。交集不足记弃权，用相同期的单集合为对照。";
            case "R05": return "沿失败簇、专家处方、AST、子代逐键追踪，记录首个缺失键，确认出生提案被真实执行。";
            case "R06": return "按相同行为窗口对照保留和移除该原子的候选，读取实际PRUNE_COMPONENT操作及其子代。";
            case "R07": return "检查两个合法组件与同角色父代，缺任一即列组件ID，齐备后比A、B、A+B并计成本。";
            case "R08": return "在事前冻结的探针集比较行为哈希和误差相关，复核BirthGate的去重或保留回执。";
            case "R09": return "将专家处方绑定到实际出生的AST和子代并复核后续支持，只有文本时定位首个未执行环节。";
            case "R10": return "比较同计算预算的变异算子、时间成本与同机会子代收益，回读下一轮实际分配。";
            case "R11": return "只读核对父子同一目标冻结和确认原件，显示负差与区间，标明缺配对原因。";
            case "R12": return "核实两个父代同目标与共同失败域，读新融合子代独立身份，用最佳单体与固定组合交叉比较。";
            case "R13": return "按冻结前固定校准器复核支持域、加权误差和BSS，列出域外弃权及受影响的研究候选。";
            case "R14": return "只读串联已提交权重、十位置概率面、调用模型哈希和冻结引用，列出第一条不匹配。";
            case "R15": return "按同机会比较无回放、固定回放和候选回放模型的遗忘与代价，记录后续回放选择。";
            case "R16": return "复核每类计算预算、CPU和唯一有效子代数，比较下一轮真实发放额度，重试去重。";
            case "R17": return "用失败机制身份匹配重复消耗，验证预算调整及活跃模型保护，保留负例。";
            case "R18": return "只读串联处方、子代、证书、唯一决策和结算；有拒绝时给出拒绝者和对应源行。";
            default: throw new IllegalArgumentException("R33_UNKNOWN_RESEARCH_VIEW:" + view);
        }
    }
}
