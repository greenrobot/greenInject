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

import de.greenrobot.inject.annotation.Value;

public class TestViewModel {
    @Value(bindTo = R.id.textView1)
    String text;
    @Value(bindTo = R.id.button1)
    String buttonText;
    @Value(bindTo = R.id.editText1)
    String editText;
}
