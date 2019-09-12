/*
 * Copyright 2018-2019 Devsoap Inc.
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
import com.devsoap.spock.Smoke
import com.devsoap.vaadinflow.ClientDependenciesTest
import com.devsoap.vaadinflow.LegacyClientDependenciesTest
import com.devsoap.vaadinflow.LegacyClientProductionModeConfigTest
import com.devsoap.vaadinflow.LegacyClientProductionModeTest
import com.devsoap.vaadinflow.FunctionalTest
import com.devsoap.vaadinflow.MultimoduleFunctionalTest

runner {
    include FunctionalTest, MultimoduleFunctionalTest
    exclude LegacyClientDependenciesTest, LegacyClientProductionModeTest, LegacyClientProductionModeConfigTest,
            ClientDependenciesTest, Smoke
}
