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
import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import de.greenrobot.inject.annotation.InjectExtra;
import de.greenrobot.inject.annotation.InjectResource;
import de.greenrobot.inject.annotation.InjectView;
import de.greenrobot.inject.annotation.OnClick;

/**
 * Injects views, resources, extras, etc. into Android activities and arbitrary Java objects.
 * 
 * @author Markus
 */
public class Injector {
    public static boolean LOG_PERFORMANCE;

    protected final Context context;
    protected final Object target;
    protected final Activity activity;
    protected final Resources resources;
    protected final Class<?> clazz;
    private final Bundle extras;

    private ValueBinder valueBinder;

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

    /** Injects into fields and wires methods. */
    public void injectAll() {
        injectFields();
        bindMethods();
    }

    /** Injects into fields. */
    public void injectFields() {
        long start = System.currentTimeMillis();
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
                } else if (annotation.annotationType() == InjectExtra.class) {
                    if (extras != null) {
                        Object value = extras.get(((InjectExtra) annotation).key());
                        injectIntoField(field, value);
                    }
                }
            }
        }
        if (LOG_PERFORMANCE) {
            long time = System.currentTimeMillis() - start;
            Log.d("greenInject", "Injected fields in " + time + "ms (" + fields.length + " fields checked)");
        }
    }

    /** Wires OnClickListeners to methods. */
    public void bindMethods() {
        long start = System.currentTimeMillis();
        Method[] methods = clazz.getDeclaredMethods();
        Set<View> modifiedViews = new HashSet<View>();
        for (final Method method : methods) {
            Annotation[] annotations = method.getAnnotations();
            for (Annotation annotation : annotations) {
                if (annotation.annotationType() == OnClick.class) {
                    bindOnClickListener(method, (OnClick) annotation, modifiedViews);
                }
            }
        }
        if (LOG_PERFORMANCE) {
            long time = System.currentTimeMillis() - start;
            Log.d("greenInject", "Bound methods in " + time + "ms (" + methods.length + " methods checked)");
        }
    }

    protected boolean bindOnClickListener(final Method method, OnClick onClick, Set<View> modifiedViews) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        boolean invokeWithView;
        if (parameterTypes.length == 0) {
            invokeWithView = false;
        } else if (parameterTypes.length == 1) {
            if (parameterTypes[0] == View.class) {
                invokeWithView = true;
            } else {
                throw new InjectException("Method may have no parameter or a single View parameter only: "
                        + method.getName() + ", found paramter type " + parameterTypes[0]);
            }
        } else {
            throw new InjectException("Method may have no parameter or a single View parameter only: "
                    + method.getName());
        }
        method.setAccessible(true);
        InjectedOnClickListener listener = new InjectedOnClickListener(target, method, invokeWithView,
                onClick.newThread());

        int[] ids = { onClick.id(), onClick.id2(), onClick.id3(), onClick.id4(), onClick.id5(), onClick.id6(),
                onClick.id7(), onClick.id8(), onClick.id9(), onClick.id10() };
        for (int id : ids) {
            if (id != 0) {
                View view = findView(method, id);
                boolean modified = modifiedViews.add(view);
                if (!modified) {
                    throw new InjectException("View can be bound to methods only once using OnClick: "
                            + method.getName());
                }
                view.setOnClickListener(listener);
            }
        }
        return invokeWithView;
    }

    protected Object findResource(Class<?> type, Member field, InjectResource annotation) {
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

    protected void injectIntoField(Field field, Object value) {
        try {
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new InjectException("Could not inject into field " + field.getName(), e);
        }
    }

    protected View findView(Member field, int viewId) {
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

    private void checkValueBinder() {
        if (valueBinder == null) {
            if (activity == null) {
                throw new InjectException("Value binding requires an activity");
            }
            valueBinder = new ValueBinder(activity, target);
        }
    }

    /** Convenience for {@link ValueBinder#valuesToUi()}. */
    public void valuesToUi() {
        checkValueBinder();
        valueBinder.valuesToUi();
    }

    /** Convenience for {@link ValueBinder#valuesToUi()}. */
    public void uiToValues() {
        checkValueBinder();
        valueBinder.uiToValues();
    }

}
