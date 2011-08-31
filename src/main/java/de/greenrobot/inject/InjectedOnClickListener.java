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

import android.view.View;
import android.view.View.OnClickListener;

import java.lang.reflect.Method;

class InjectedOnClickListener implements OnClickListener {

    private final Object target;
    private final Method method;
    private final boolean invokeWithViewParam;
    private final boolean invokeInNewThread;

    InjectedOnClickListener(Object target, Method method, boolean invokeWithViewParam, boolean invokeInNewThread) {
        this.target = target;
        this.method = method;
        this.invokeWithViewParam = invokeWithViewParam;
        this.invokeInNewThread = invokeInNewThread;
    }

    @Override
    public void onClick(final View view) {
        if (invokeInNewThread) {
            new Thread() {
                @Override
                public void run() {
                    handleOnClick(view);
                }
            }.start();
        } else {
            handleOnClick(view);
        }
    }

    protected void handleOnClick(View view) {
        try {
            if (invokeWithViewParam) {
                method.invoke(target, view);
            } else {
                method.invoke(target);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
