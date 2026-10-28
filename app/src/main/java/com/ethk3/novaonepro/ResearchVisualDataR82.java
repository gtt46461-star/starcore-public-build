package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable, bounded, read-only chart transport. NaN means unavailable, never zero. */
public final class ResearchVisualDataR82 {
    private ResearchVisualDataR82() { }
    public static final int MAX_POINTS = 120;
    public static final int MAX_EDGES = 120;
    public static final class Point {
        public final String label, details;
        public final double value, lower, upper, open, expected;
        public final long n;
        /** R27: optional per-point component values (stacked bars / multi-line series); null when unused. */
        public final double[] stack;
        public Point(String label, double value, double lower, double upper, long n, String details, double open, double expected, double[] stack) {
            this.label=safe(label); this.value=value; this.lower=lower; this.upper=upper;
            if(n < -1)throw new IllegalArgumentException("sample n"); this.n=n; this.details=safe(details); this.open=open; this.expected=expected; this.stack=stack==null?null:stack.clone();
        }
        public Point(String label, double value, double lower, double upper, long n, String details) {
            this(label,value,lower,upper,n,details,Double.NaN,Double.NaN);
        }
        public Point(String label, double value, double lower, double upper, long n, String details, double open) {
            this(label,value,lower,upper,n,details,open,Double.NaN);
        }
        public Point(String label, double value, double lower, double upper, long n, String details, double open, double expected) {
            this.label=safe(label); this.value=value; this.lower=lower; this.upper=upper;
            if(n < -1)throw new IllegalArgumentException("sample n"); this.n=n; this.details=safe(details); this.open=open; this.expected=expected; this.stack=null;
        }
    }
    public static final class Edge {
        public final String from,to,label;
        public Edge(String from,String to,String label) { this.from=safe(from);this.to=safe(to);this.label=safe(label); }
    }
    public static final class Panel {
        public final String page,title,kind,source,window,note,status;
        public final List<Point> points;
        public final List<Edge> edges;
        /** R27: horizontal reference lines (e.g. 30% baseline, luck ceiling) and series legend names. */
        public final List<Double> refs; public final List<String> series;
        public Panel(String page,String title,String kind,String source,String window,String note,String status,List<Point> points,List<Edge> edges,List<Double> refs,List<String> series) {
            this.page=safe(page);this.title=safe(title);this.kind=safe(kind);this.source=safe(source);
            this.window=safe(window);this.note=safe(note);this.status=safe(status);
            this.points=bounded(points,MAX_POINTS);this.edges=bounded(edges,MAX_EDGES);this.refs=bounded(refs,8);this.series=bounded(series,12);
        }
        public Panel(String page,String title,String kind,String source,String window,long ignoredN,String note,String status,List<Point> points,List<Edge> edges) {
            this(page,title,kind,source,window,note,status,points,edges);
        }
        public Panel(String page,String title,String kind,String source,String window,String note,String status,List<Point> points,List<Edge> edges) {
            this.page=safe(page);this.title=safe(title);this.kind=safe(kind);this.source=safe(source);
            this.window=safe(window);this.note=safe(note);this.status=safe(status);
            this.points=bounded(points,MAX_POINTS);this.edges=bounded(edges,MAX_EDGES);this.refs=Collections.emptyList();this.series=Collections.emptyList();
        }
    }
    public static final class Dashboard {
        public final String page,status,asOfIssue,exactCandidate,candidateKey,dataVersion,sourceVersion,note,snapshotId;
        public final long generatedAt;
        public final List<Panel> panels;
        public Dashboard(String page,String status,String asOfIssue,String exactCandidate,String candidateKey,String dataVersion,String sourceVersion,String note,List<Panel> panels) {
            this(page,status,asOfIssue,exactCandidate,candidateKey,dataVersion,sourceVersion,note,"",System.currentTimeMillis(),panels);
        }
        public Dashboard(String page,String status,String asOfIssue,String exactCandidate,String candidateKey,String dataVersion,String sourceVersion,String note,String snapshotId,long generatedAt,List<Panel> panels) {
            this.page=safe(page);this.status=safe(status);this.asOfIssue=safe(asOfIssue);
            this.exactCandidate=safe(exactCandidate);this.candidateKey=safe(candidateKey);
            this.dataVersion=safe(dataVersion);this.sourceVersion=safe(sourceVersion);this.note=safe(note);
            this.snapshotId=safe(snapshotId);this.generatedAt=generatedAt;this.panels=bounded(panels,40);
        }
    }
    private static String safe(String value) { return value==null?"":value; }
    private static <T> List<T> bounded(List<T> source,int cap) {
        if(source==null||source.isEmpty())return Collections.emptyList();
        if(source.size()>cap)throw new IllegalArgumentException("chart transport exceeds "+cap);
        return Collections.unmodifiableList(new ArrayList<T>(source));
    }
}
