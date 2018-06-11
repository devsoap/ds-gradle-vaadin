/*
 * Copyright 2018 Devsoap Inc.
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
package com.devsoap.vaadinflow.models

/**
 * TemplateModel for representing a Web Component
 *
 * @author John Ahlroos
 * @since 1.0
 */
class WebComponent extends Component {

    PackageManager packageManager

    String dependencyPackage

    String dependencyHtml

    enum PackageManager {
        YARN('node_modules'),
        BOWER('bower_components')

        private final String pkg
        PackageManager(String pkg) {
            this.pkg = pkg
        }

        String getPackage() {
            pkg
        }
    }
}
