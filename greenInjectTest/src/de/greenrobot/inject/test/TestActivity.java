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

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import de.greenrobot.inject.annotation.InjectExtra;
import de.greenrobot.inject.annotation.InjectResource;
import de.greenrobot.inject.annotation.InjectView;
import de.greenrobot.inject.annotation.OnClick;
import de.greenrobot.inject.annotation.Value;

public class TestActivity extends Activity {

    @InjectView(id = R.id.textView1)
    TextView textView;
    View textViewReference;

    @InjectView(id = R.id.listView)
    ListView listView;
    ListView listViewReference;
    
    @InjectResource(id = R.string.app_name)
    String app_name;

    @InjectResource(id = R.drawable.icon)
    BitmapDrawable icon;

    @InjectResource(id = R.drawable.icon)
    Bitmap iconBitmap;

    boolean button1Clicked;
    View clickedView;

    @Value(bindTo = R.id.editText1)
    String value;

    @InjectExtra(key = "color")
    String color;

    int clickCount;
    Thread clickThread;

    @Value(bindTo = R.id.imageView1)
    int imageResId1;

    @Value(bindTo = R.id.imageView2)
    Bitmap imageBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        textViewReference = findViewById(R.id.textView1);
        listViewReference = (ListView)findViewById(R.id.listView);

        listViewReference.setAdapter(new TestListAdapter(this));
    }

    @OnClick(id = R.id.button1)
    void clickMe() {
        button1Clicked = true;
        clickThread = Thread.currentThread();
    }

    @OnClick(id = R.id.button2)
    void clickMe(View clickedView) {
        this.clickedView = clickedView;
        clickThread = Thread.currentThread();
    }

    @OnClick(id = R.id.button3, id2 = R.id.button4)
    void increaseClickCount() {
        clickCount++;
        clickThread = Thread.currentThread();
    }

    @OnClick(id = R.id.button5, newThread = true)
    void clickNewThread() {
        clickThread = Thread.currentThread();
    }

}
