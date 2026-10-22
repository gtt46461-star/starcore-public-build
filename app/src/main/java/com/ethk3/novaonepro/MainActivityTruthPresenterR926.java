package com.ethk3.novaonepro;

/** Pure presenter for the full-truth/detail pages opened from MainActivity. */
public final class MainActivityTruthPresenterR926 {
    private static final String[] OPERATION_TITLES =
            new String[] {
                "实时流水", "抓取记录", "影子统计", "研究证书漏斗", "淘汰原因明细", "流水CSV"
            };

    public static final class RenderedDetail {
        public final String title;
        public final String body;
        public final boolean csv;

        RenderedDetail(String title, String body, boolean csv) {
            this.title = safe(title);
            this.body = safe(body);
            this.csv = csv;
        }
    }

    private MainActivityTruthPresenterR926() {}

    public static RenderedDetail render(DbStore store, int kind) {
        RuntimeReadOnlyRepositoryR926 repository = new RuntimeReadOnlyRepositoryR926(store);
        if(kind==3||kind==9){RuntimeTruthSnapshotR906 truth=HomeTruthRepositoryR906.capture(store);return new RenderedDetail("业务漏斗14阶段完整明细",truth.businessFunnel==null?"READ_ERROR|"+truth.readError:truth.businessFunnel.detail(),false);}
        if (kind >= 1 && kind <= 4) {
            int section = kind - 1;
            return new RenderedDetail(
                    ResearchDashboardR924.TITLES[section],
                    repository.researchDashboardSection(section),
                    false);
        }
        if (kind >= 6 && kind <= 11) {
            int operation = kind - 6;
            String body;
            switch (kind) {
                case 6:
                    body = repository.pipeline(500, false);
                    break;
                case 7:
                    body = repository.fetches(100);
                    break;
                case 8:
                    body = repository.shadow();
                    break;
                case 9:
                    body = repository.researchReadiness();
                    break;
                case 10:
                    body = repository.lifecycleAudit();
                    break;
                case 11:
                    body = repository.pipeline(500, true);
                    break;
                default:
                    throw new IllegalStateException("R926_UNREACHABLE_DETAIL_KIND:" + kind);
            }
            return new RenderedDetail(OPERATION_TITLES[operation], body, kind == 11);
        }
        return new RenderedDetail(
                "5.4.1.0.1 运行、结算与真实出手", repository.combinedRuntimeDetail(), false);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
