/*
 * Copyright 2018-2020 Devsoap Inc.
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0
 * International Public License (the "License"); you may not use this file
 * except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *      https://creativecommons.org/licenses/by-nd/4.0/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.devsoap.vaadinflow.actions

import org.gradle.api.Project

/**
 * Deprecated old Gretty Plugin
 *
 * @author John Ahlroos
 * @since 1.0
 */
class GrettyDeprecatedPluginAction extends PluginAction {

    final String pluginId = 'org.akhikhl.gretty'

    @Override
    protected void execute(Project project) {
        throw new IllegalArgumentException('The Gretty plugin used in this project is deprecated and un-maintained ' +
                '(See https://github.com/akhikhl/gretty/issues/436). ' +
                'Replace it with the \'org.gretty\' plugin instead.')
    }
}
