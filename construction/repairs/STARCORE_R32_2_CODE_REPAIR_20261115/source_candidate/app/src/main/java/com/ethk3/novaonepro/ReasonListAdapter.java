package com.ethk3.novaonepro;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public final class ReasonListAdapter extends BaseAdapter {
    private final LayoutInflater inflater;
    private final ArrayList<Row> rows = new ArrayList<Row>();

    public ReasonListAdapter(Context c) {
        inflater = LayoutInflater.from(c);
    }

    public void replace(List<Row> x) {
        rows.clear();
        if (x != null) rows.addAll(x);
        notifyDataSetChanged();
    }

    public int getCount() {
        return rows.size();
    }

    public Object getItem(int p) {
        return rows.get(p);
    }

    public long getItemId(int p) {
        return rows.get(p).id;
    }

    public boolean hasStableIds() {
        return true;
    }

    public View getView(int p, View v, ViewGroup parent) {
        Holder h;
        if (v == null) {
            v = inflater.inflate(R.layout.row_reason, parent, false);
            h = new Holder();
            h.title = (TextView) v.findViewById(R.id.txt_reason_title);
            h.detail = (TextView) v.findViewById(R.id.txt_reason_detail);
            v.setTag(h);
        } else h = (Holder) v.getTag();
        Row r = rows.get(p);
        h.title.setText(r.title);
        h.detail.setText(r.detail);
        h.detail.setMaxLines(8);
        return v;
    }

    static final class Holder {
        TextView title, detail;
    }

    public static final class Row {
        public long id;
        public String title = "", detail = "";
    }
}
