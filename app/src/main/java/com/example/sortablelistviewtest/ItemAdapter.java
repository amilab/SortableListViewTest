package com.example.sortablelistviewtest;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class ItemAdapter extends ArrayAdapter<Item> {
	private Context context;
	private List<Item> items;
	private LayoutInflater inflater;

	public ItemAdapter(Context context, int textViewResourceId, List<Item> items) {
		super(context, textViewResourceId, items);
		
		this.context = context;
		this.items = items;
		this.inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		final View view;
		if (convertView == null) {
			view = inflater.inflate(R.layout.item, parent, false);
		} else {
			view = convertView;
		}

		final Item item = items.get(position);
		if (item != null) {
			final TextView textTitle = (TextView)view.findViewById(R.id.text_title);
			textTitle.setText(item.getTitle());
		}
		
		return view;
	}
}
