package android.widget;

import android.view.View;
import android.view.ViewGroup;

public class AdapterView<T extends Adapter> extends ViewGroup {
    public interface OnItemClickListener {
        void onItemClick(AdapterView<?> parent, View view, int position, long id);
    }
    public void setOnItemClickListener(OnItemClickListener listener) {}
}
