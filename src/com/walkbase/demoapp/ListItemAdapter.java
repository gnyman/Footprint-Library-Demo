package com.walkbase.demoapp;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.walkbase.positioning.data.WalkbaseLocation;

public class ListItemAdapter extends ArrayAdapter<WalkbaseLocation> {
	private final Context context;
	private final List<WalkbaseLocation> objects;
	
	

	public ListItemAdapter(Context context, int textViewResourceId, List<WalkbaseLocation> objects) {
		super(context, textViewResourceId, objects);
		this.context = context;
		this.objects = objects;
	}
	
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView = inflater.inflate(R.layout.list_item, parent, false);
		TextView textView = (TextView) rowView.findViewById(R.id.locationName);
		TextView detailView = (TextView) rowView.findViewById(R.id.locationDetails);
		WalkbaseLocation o = this.objects.get(position);
		textView.setText(o.locationName);
		detailView.setText("Details:\nid:"+o.locationId+" listId:" + o.listId+" pos:"+o.latitude+","+o.longitude+"+/-"+o.accuracy+" score:"+o.score+" extra:"+o.extra);	
		return rowView;
	}
}
