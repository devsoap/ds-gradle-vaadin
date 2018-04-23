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

import groovy.transform.builder.Builder

/**
 * Model for representing a Web Component
 *
 * @author John Ahlroos
 * @since 1.0
 */
@Builder
class WebComponent {

    String componentName

    String componentPackage

    String componentTag

    File rootDirectory

    String dependencyPackage

    String dependencyHtml
}
