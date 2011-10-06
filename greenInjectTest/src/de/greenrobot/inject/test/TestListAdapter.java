package de.greenrobot.inject.test;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import de.greenrobot.inject.Injector;

public class TestListAdapter extends BaseAdapter {

	TestItemHolder[] items = new TestItemHolder[]{
			new TestItemHolder(0, "Label0"),
			new TestItemHolder(1, "Label1")
	};
	
	private Context context;
	private LayoutInflater inflater;
	
	public TestListAdapter(Context context) {
		this.context = context;
		inflater = LayoutInflater.from(context);
	}
	@Override
	public int getCount() {
		return items.length;
	}

	@Override
	public Object getItem(int index) {
		return items[index];
	}

	@Override
	public long getItemId(int position) {
		return items[position].getIndex();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if(convertView == null) {
			convertView = inflater.inflate(R.layout.item, null);
			Injector injector = Injector.inject(context, convertView, items[position]);
			injector.valuesToUi();
		}
		return convertView;
	}

}
