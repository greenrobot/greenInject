/*
 * Copyright (C) 2011 Markus Junginger, greenrobot (http://greenrobot.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.greenrobot.inject.test;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Debug;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import de.greenrobot.inject.Injector;

public class ActivityInjectTest extends ActivityInstrumentationTestCase2<TestInheritedActivity> {
    private final boolean methodTracing = false;

    public ActivityInjectTest() {
        super("de.greenrobot.inject.test", TestInheritedActivity.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
        Injector.LOG_PERFORMANCE = true;
        System.gc();
        if (methodTracing) {
            Debug.startMethodTracing(getName());
        }
    }

    @Override
    protected void tearDown() throws Exception {
        if (methodTracing) {
            Debug.stopMethodTracing();
        }
        System.gc();
        super.tearDown();
    }

    public void testInject() {
        TestActivity activity = getActivity();
        assertNotNull(activity.textViewReference);
        assertNotNull(activity.listViewReference);
        assertNull(activity.textView);
        assertNull(activity.listView);
        assertNull(activity.app_name);
        assertNull(activity.icon);
        Injector.injectInto(activity);
        assertSame(activity.textViewReference, activity.textView);
        assertSame(activity.listViewReference, activity.listView);
        assertEquals(activity.getString(R.string.app_name), activity.app_name);
        BitmapDrawable drawable = (BitmapDrawable) activity.getResources().getDrawable(R.drawable.icon);
        assertEquals(drawable.getBitmap().getHeight(), activity.icon.getBitmap().getHeight());

        assertEquals(drawable.getBitmap().getHeight(), activity.iconBitmap.getHeight());
    }

    @UiThreadTest
    public void testClick() {
        TestActivity activity = getActivity();
        Injector.injectInto(activity);
        assertFalse(activity.button1Clicked);
        View button = activity.findViewById(R.id.button1);
        assertTrue(button.performClick());
        assertTrue(activity.button1Clicked);
    }

    @UiThreadTest
    public void testItemClick() {
        TestActivity activity = getActivity();
        Injector.injectInto(activity);
        
        ListView list = (ListView)activity.findViewById(R.id.listView);
        ListAdapter adapter = list.getAdapter();
        TestItemHolder item0 = (TestItemHolder) adapter.getItem(0);
        TestItemHolder item1 = (TestItemHolder) adapter.getItem(1);
        assertFalse(item0.clicked);
        assertFalse(item1.clicked);
        
        list.setSelection(0);
        View itemView = list.getSelectedView();
        Button button = (Button) itemView.findViewById(R.id.itemButton);
        assertTrue(button.performClick());
        
        assertTrue(item0.clicked);
        assertFalse(item1.clicked);
    }

    @UiThreadTest
    public void testClickTwoViews() {
        TestActivity activity = getActivity();
        Injector.injectInto(activity);
        assertEquals(0, activity.clickCount);
        assertTrue(activity.findViewById(R.id.button3).performClick());
        assertTrue(activity.findViewById(R.id.button4).performClick());
        assertEquals(2, activity.clickCount);
    }

    @UiThreadTest
    public void testClickWithView() {
        TestActivity activity = getActivity();
        Injector.injectInto(activity);
        assertNull(activity.clickedView);
        View button = activity.findViewById(R.id.button2);
        assertTrue(button.performClick());
        assertEquals(button, activity.clickedView);
    }

    @UiThreadTest
    public void testClickNewThread() throws InterruptedException {
        Thread uiThread = Thread.currentThread();
        TestActivity activity = getActivity();
        Injector.injectInto(activity);
        assertNull(activity.clickThread);

        assertTrue(activity.findViewById(R.id.button1).performClick());
        while (activity.clickThread == null) {
            Thread.sleep(1);
        }
        assertSame(uiThread, activity.clickThread);

        activity.clickThread = null;
        assertTrue(activity.findViewById(R.id.button5).performClick());
        while (activity.clickThread == null) {
            Thread.sleep(1);
        }
        assertNotSame(uiThread, activity.clickThread);

    }

    @UiThreadTest
    public void testValue() {
        TestActivity activity = getActivity();
        Injector injector = new Injector(activity);

        assertNull(activity.value);

        activity.value = "rhino";
        injector.valuesToUi();
        
        EditText editText = (EditText) activity.findViewById(R.id.editText1);
        String valueUi = editText.getText().toString();
        assertEquals("rhino", valueUi);

        editText.setText("tiger");
        injector.uiToValues();
        assertEquals("tiger", activity.value);
    }

    @UiThreadTest
    public void testCheckBox() {
        TestActivity activity = getActivity();
        Injector injector = new Injector(activity);

        assertFalse(activity.check);

        activity.check = true;
        injector.valuesToUi();
        
        CheckBox checkBox = (CheckBox) activity.findViewById(R.id.checkBox1);
        assertTrue(checkBox.isChecked());

        checkBox.setChecked(false);
        injector.uiToValues();
        assertFalse(activity.check);
    }

    @UiThreadTest
    public void testValueImageViewResId() {
        TestActivity activity = getActivity();
        Injector injector = new Injector(activity);
        ImageView imageView = (ImageView) activity.findViewById(R.id.imageView1);
        Drawable drawable = imageView.getDrawable();
        assertNull(drawable);

        activity.imageResId1 = R.drawable.icon;
        injector.valuesToUi();

        drawable = imageView.getDrawable();
        assertNotNull(drawable);

        injector.uiToValues();
        assertEquals(R.drawable.icon, activity.imageResId1);

        activity.imageResId1 = 0;
        injector.valuesToUi();

        drawable = imageView.getDrawable();
        assertNull(drawable);

    }

    @UiThreadTest
    public void testValueImageViewBitmap() {
        TestActivity activity = getActivity();
        Injector injector = new Injector(activity);
        ImageView imageView = (ImageView) activity.findViewById(R.id.imageView2);
        Drawable drawable = imageView.getDrawable();
        assertNull(drawable);

        Bitmap bitmap = BitmapFactory.decodeResource(getActivity().getResources(), R.drawable.icon);
        activity.imageBitmap = bitmap;
        injector.valuesToUi();

        drawable = imageView.getDrawable();
        assertNotNull(drawable);

        injector.uiToValues();
        assertEquals(bitmap, activity.imageBitmap);

        activity.imageBitmap = null;
        injector.valuesToUi();

        drawable = imageView.getDrawable();
        if (drawable != null) {
            assertNull(((BitmapDrawable) drawable).getBitmap());
        }
    }

    public void testExtra() {
        Intent intent = new Intent();
        intent.putExtra("color", "green");
        setActivityIntent(intent);
        TestActivity activity = getActivity();
        Injector.injectInto(activity);
        assertEquals("green", activity.color);
    }

    @UiThreadTest
    public void testViewModelValues() {
        TestActivity activity = getActivity();
        TestViewModel model = new TestViewModel();
        Injector injector = new Injector(activity, model);

        assertNull(activity.value);

        model.buttonText = "PressMe";
        model.editText = "EditMe";
        model.text = "ReadMe";
        injector.valuesToUi();

        TextView textView = (TextView) activity.findViewById(R.id.textView1);
        EditText editText = (EditText) activity.findViewById(R.id.editText1);
        Button button = (Button) activity.findViewById(R.id.button1);

        assertEquals("ReadMe", textView.getText().toString());
        assertEquals("EditMe", editText.getText().toString());
        assertEquals("PressMe", button.getText().toString());

        textView.setText("text");
        editText.setText("edit");
        button.setText("button");
        injector.uiToValues();
        assertEquals("text", model.text);
        assertEquals("edit", model.editText);
        assertEquals("button", model.buttonText);
    }

}
