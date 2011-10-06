package de.greenrobot.inject.test;

import de.greenrobot.inject.annotation.OnClick;
import de.greenrobot.inject.annotation.Value;

public class TestItemHolder {

	private int index;

	@Value(bindTo=R.id.itemLabel)
	String label;
	
	boolean clicked = false;

	public TestItemHolder(int index, String label) {
		this.index = index;
		this.label = label;
	}
	
	@OnClick(id=R.id.itemButton)
	public void itemButtonClicked() {
		clicked = true;
	}
	
	public int getIndex() {
		return index;
	}
	
}
