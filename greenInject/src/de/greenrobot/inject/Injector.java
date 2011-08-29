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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import de.greenrobot.inject.annotation.Extra;
import de.greenrobot.inject.annotation.InjectResource;
import de.greenrobot.inject.annotation.InjectView;
import de.greenrobot.inject.annotation.OnClick;
import de.greenrobot.inject.annotation.Value;

/**
 * Injects views, resources, extras, etc. into Android activities and arbitrary Java objects.
 * 
 * @author Markus
 */
public class Injector {

    protected final Context context;
    protected final Object target;
    protected final Activity activity;
    protected final Resources resources;
    protected final Class<?> clazz;
    protected List<Field> valueFields;
    protected List<View> valueViews;
    private final Bundle extras;

    public Injector(Context context) {
        this(context, context);
    }

    public Injector(Context context, Object target) {
        if (context == null || target == null) {
            throw new IllegalArgumentException("Context/target may not be null");
        }
        this.context = context;
        this.target = target;
        resources = context.getResources();
        if (context instanceof Activity) {
            activity = (Activity) context;
            Intent intent = activity.getIntent();
            if (intent != null) {
                extras = intent.getExtras();
            } else {
                extras = null;
            }
        } else {
            activity = null;
            extras = null;
        }
        clazz = target.getClass();
    }

    public static Injector injectInto(Context context) {
        return inject(context, context);
    }

    public static Injector inject(Context context, Object target) {
        Injector injector = new Injector(context, target);
        injector.injectAll();
        return injector;
    }

    public void injectAll() {
        long start = System.currentTimeMillis();
        injectFields();
        injectMethods();
        long time = System.currentTimeMillis() - start;
        System.out.println(">>>" + time + "ms");
    }

    public void injectFields() {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            Annotation[] annotations = field.getAnnotations();
            for (Annotation annotation : annotations) {
                if (annotation.annotationType() == InjectView.class) {
                    int id = ((InjectView) annotation).id();
                    View view = findView(field, id);
                    injectIntoField(field, view);
                } else if (annotation.annotationType() == InjectResource.class) {
                    Object ressource = findResource(field.getType(), field, (InjectResource) annotation);
                    injectIntoField(field, ressource);
                } else if (annotation.annotationType() == Extra.class) {
                    if (extras != null) {
                        Object value = extras.get(((Extra) annotation).key());
                        injectIntoField(field, value);
                    }
                }
            }
        }
    }

    public Object findExtra(Extra annotation) {
        if (extras == null) {
            throw new InjectException("No intent extras available for extras");
        }
        return extras.get(annotation.key());
    }

    public void injectMethods() {
        Method[] methods = clazz.getDeclaredMethods();
        for (final Method method : methods) {
            Annotation[] annotations = method.getAnnotations();
            for (Annotation annotation : annotations) {
                if (annotation.annotationType() == OnClick.class) {
                    int id = ((OnClick) annotation).id();
                    View view = findView(method, id);
                    setOnClickListener(view, method);
                }
            }
        }
    }

    public void setOnClickListener(View view, final Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length == 0) {
            method.setAccessible(true);
            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        method.invoke(target);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        } else if (parameterTypes.length == 1) {
            if (parameterTypes[0] == View.class) {
                method.setAccessible(true);
                view.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            method.invoke(target, v);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            } else {
                throw new InjectException("Method may have no parameter or a single View parameter only: "
                        + method.getName() + ", found paramter type " + parameterTypes[0]);
            }
        } else {
            throw new InjectException("Method may have no parameter or a single View parameter only: "
                    + method.getName());
        }
    }

    private Object findResource(Class<?> type, Member field, InjectResource annotation) {
        int id = annotation.id();
        if (type == String.class) {
            return context.getString(id);
        } else if (Drawable.class.isAssignableFrom(type)) {
            return resources.getDrawable(id);
        } else if (Bitmap.class.isAssignableFrom(type)) {
            return BitmapFactory.decodeResource(resources, id);
        } else {
            throw new InjectException("Cannot inject for type " + type + " (field " + field.getName() + ")");
        }
    }

    public void injectIntoField(Field field, Object value) {
        try {
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new InjectException("Could not inject into field " + field.getName(), e);
        }
    }

    public View findView(Member field, int viewId) {
        if (activity == null) {
            throw new InjectException("Views can be injected only in activities (member " + field.getName() + " in "
                    + context.getClass());
        }
        View view = activity.findViewById(viewId);
        if (view == null) {
            throw new InjectException("View not found for member " + field.getName());
        }
        return view;
    }

    public void valuesToUi() {
        checkValueFields();

        for (int i = 0; i < valueFields.size(); i++) {
            Field field = valueFields.get(i);
            View view = valueViews.get(i);

            Object value;
            try {
                value = field.get(target);
            } catch (Exception e) {
                throw new InjectException("Could not get value for field " + field.getName(), e);
            }
            if (view instanceof TextView) {
                ((TextView) view).setText(value != null ? value.toString() : null);
            }
        }
    }

    public void uiToValues() {
        checkValueFields();

        for (int i = 0; i < valueFields.size(); i++) {
            Field field = valueFields.get(i);
            View view = valueViews.get(i);

            if (view instanceof TextView) {
                String value = ((TextView) view).getText().toString();
                injectIntoField(field, value);
            }
        }
    }

    protected void checkValueFields() {
        if (valueFields == null) {
            valueFields = new ArrayList<Field>();
            valueViews = new ArrayList<View>();
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                Annotation[] annotations = field.getAnnotations();
                for (Annotation annotation : annotations) {
                    if (annotation.annotationType() == Value.class) {
                        field.setAccessible(true);
                        valueFields.add(field);
                        int viewId = ((Value) annotation).bindTo();
                        View view = findView(field, viewId);
                        valueViews.add(view);
                    }
                }
            }
        }
    }

    public void refreshUiViews() {
        if (valueFields != null) {
            valueViews.clear();
            for (Field field : valueFields) {
                Annotation[] annotations = field.getAnnotations();
                for (Annotation annotation : annotations) {
                    if (annotation.annotationType() == Value.class) {
                        int viewId = ((Value) annotation).bindTo();
                        View view = findView(field, viewId);
                        valueViews.add(view);
                    }
                }
            }
        }
    }

}
