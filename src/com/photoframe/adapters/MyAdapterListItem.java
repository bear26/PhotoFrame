package com.photoframe.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.photoframe.R;
import com.yandex.disk.client.ListItem;

public class MyAdapterListItem<T> extends ArrayAdapter<ListItem> {
    Context context;

    public MyAdapterListItem(Context context, int resource) {
        super(context, resource);
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ListItem item = getItem(position);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.list_item, null);
        TextView textView = (TextView) view.findViewById(R.id.listItemTextView);
        if (item.isCollection()) textView.setTextColor(Color.YELLOW);
        textView.setText(item.getDisplayName());
        return view;
    }

}
