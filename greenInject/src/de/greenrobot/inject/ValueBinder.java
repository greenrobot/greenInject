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
package de.greenrobot.inject;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import de.greenrobot.inject.annotation.Value;

/**
 * "Binds" values (fields) to UI views. Setting UI/field values must be triggered explicitly by
 * {@link ValueBinder#valuesToUi()} or {@link ValueBinder#uiToValues()}.
 * 
 * @author Markus
 */
public class ValueBinder {
    protected static Map<Class<?>, List<Field>> valueFieldsForClass = new ConcurrentHashMap<Class<?>, List<Field>>();
    protected static Map<Class<?>, List<Integer>> valueViewIdsForClass = new ConcurrentHashMap<Class<?>, List<Integer>>();

    protected final Object target;
    protected final Activity activity;
	protected final View ui;

    protected List<Field> valueFields;
    protected List<View> valueViews;
    protected List<Integer> valueViewIds;

    private Class<? extends Object> clazz;

    /** If the value fields are in the activity itself. */
    public ValueBinder(Activity activity) {
        this(activity, activity);
    }

    /** If the value fields are in a object different from the activity. */
    public ValueBinder(Activity activity, Object target) {
    	this(activity, null, target);
    }
    
    /** If the value fields are in a object different from the activity. */
    public ValueBinder(Activity activity, View ui, Object target) {
        if (activity == null || target == null) {
            throw new IllegalArgumentException("Context/target may not be null");
        }
        this.activity = activity;
        this.ui = ui;
        this.target = target;
        clazz = target.getClass();
    }

    protected View findView(Member field, int viewId) {
        View view = null;
        if (ui != null) {
        	view = ui.findViewById(viewId);
        }
        if (view == null) {
        	view = activity.findViewById(viewId);
        }
        if (view == null) {
            throw new InjectException("View not found for member " + field.getName());
        }
        return view;
    }

    /** Applies the values annotated with @Value to the UI views. */
    public void valuesToUi() {
        long start = System.currentTimeMillis();
        checkValueFields();
        long start2 = System.currentTimeMillis();

        for (int i = 0; i < valueFields.size(); i++) {
            Field field = valueFields.get(i);
            View view = valueViews.get(i);

            Object value;
            try {
                value = field.get(target);
            } catch (Exception e) {
                throw new InjectException("Could not get value for field " + field.getName(), e);
            }
            if (view instanceof CompoundButton) {
            	if( value != null ) {
            		((CompoundButton) view).setChecked( ((Boolean)value).booleanValue() );
            	}
            } else if (view instanceof TextView) {
                    ((TextView) view).setText(value != null ? value.toString() : null);
            } else if (view instanceof ImageView) {
                ImageView imageView = (ImageView) view;
                if (value == null || value instanceof Bitmap) {
                    imageView.setImageBitmap((Bitmap) value);
                } else if (value instanceof Integer) {
                    int resId = (Integer) value;
                    imageView.setImageResource(resId);
                } else if (value instanceof Drawable) {
                    imageView.setImageDrawable((Drawable) value);
                }
            }
        }
        if (Injector.LOG_PERFORMANCE) {
            long time = System.currentTimeMillis() - start;
            long time2 = System.currentTimeMillis() - start2;
            Log.d("greenInject", "valuesToUi proccesed " + valueFields.size() + " fields in " + time2 + "/" + time
                    + "ms");
        }
    }

    /** Reads the values annotated with @Value from the UI views. */
    public void uiToValues() {
        long start = System.currentTimeMillis();
        checkValueFields();
        long start2 = System.currentTimeMillis();

        for (int i = 0; i < valueFields.size(); i++) {
            Field field = valueFields.get(i);
            View view = valueViews.get(i);

            if (view instanceof CompoundButton) {
                boolean value = ((CompoundButton) view).isChecked();
                injectIntoField(field, value);
            } else if (view instanceof TextView) {
                String value = ((TextView) view).getText().toString();
                injectIntoField(field, value);
            }
        }
        if (Injector.LOG_PERFORMANCE) {
            long time = System.currentTimeMillis() - start;
            long time2 = System.currentTimeMillis() - start2;
            Log.d("greenInject", "uiToValues proccesed " + valueFields.size() + " fields in " + time2 + "/" + time
                    + "ms");
        }
    }

    protected void injectIntoField(Field field, Object value) {
        try {
            field.set(target, value);
        } catch (Exception e) {
            throw new InjectException("Could not inject into field " + field.getName(), e);
        }
    }

    protected void checkValueFields() {
        if (valueFields == null) {
            valueFields = valueFieldsForClass.get(clazz);
            valueViewIds = valueViewIdsForClass.get(clazz);
            if (valueFields == null || valueViewIds == null) {
                valueFields = new ArrayList<Field>();
                valueViewIds = new ArrayList<Integer>();
                Field[] fields = clazz.getDeclaredFields();
                for (Field field : fields) {
                    Value annotation = field.getAnnotation(Value.class);
                    if (annotation != null) {
                        field.setAccessible(true);
                        valueFields.add(field);
                        int viewId = ((Value) annotation).bindTo();
                        valueViewIds.add(viewId);
                    }
                }
                valueFieldsForClass.put(clazz, valueFields);
                valueViewIdsForClass.put(clazz, valueViewIds);
            }
        }
        refreshUiViews();
    }

    public void refreshUiViews() {
        if (valueViews == null) {
            valueViews = new ArrayList<View>();
        } else {
            valueViews.clear();
        }
        int size = valueFields.size();
        if (size != valueViewIds.size()) {
            throw new InjectException("Internal error; size: " + size + " vs. " + valueViewIds.size());
        }
        for (int i = 0; i < size; i++) {
            int viewId = valueViewIds.get(i);
            Field field = valueFields.get(i);
            View view = findView(field, viewId);
            valueViews.add(view);
        }
    }

}
