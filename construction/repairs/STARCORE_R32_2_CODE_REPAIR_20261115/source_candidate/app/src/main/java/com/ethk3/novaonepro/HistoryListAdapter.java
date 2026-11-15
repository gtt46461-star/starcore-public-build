package com.ethk3.novaonepro;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Reusing ListView adapter; no removeAllViews history rebuild. */
public final class HistoryListAdapter extends BaseAdapter {
    private final LayoutInflater inflater;
    private final ArrayList<Row> rows = new ArrayList<Row>();

    public HistoryListAdapter(Context context) {
        inflater = LayoutInflater.from(context);
    }

    public void replace(List<Row> values) {
        rows.clear();
        if (values != null) rows.addAll(values);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return rows.size();
    }

    @Override
    public Object getItem(int position) {
        return rows.get(position);
    }

    @Override
    public long getItemId(int position) {
        return rows.get(position).id;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.row_history, parent, false);
            holder = new Holder();
            holder.issue = (TextView) convertView.findViewById(R.id.txt_history_issue);
            holder.result = (TextView) convertView.findViewById(R.id.txt_history_result);
            holder.action = (TextView) convertView.findViewById(R.id.txt_history_decision);
            holder.probability = (TextView) convertView.findViewById(R.id.txt_history_probability);
            holder.detail = (TextView) convertView.findViewById(R.id.txt_history_detail);
            holder.reason = (TextView) convertView.findViewById(R.id.txt_history_reason);
            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }
        Row row = rows.get(position);
        holder.issue.setText(row.issue);
        holder.result.setText(row.result);
        holder.action.setText(row.action);
        holder.probability.setText(row.probability);
        holder.detail.setText(row.detail);
        holder.reason.setText(row.reason);
        holder.reason.setMaxLines(4);
        return convertView;
    }

    private static final class Holder {
        TextView issue, result, action, probability, detail, reason;
    }

    public static final class Row {
        public long id;
        public String issue = "";
        public String result = "";
        public String action = "";
        public String probability = "";
        public String detail = "";
        public String reason = "";
        public String body = "";
    }
}
