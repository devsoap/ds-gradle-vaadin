# Development Guide

To develop the plugin locally you will need to clone the sources of the plugin. 

You can do it by just cloning this repository:

```bash
$> git clone https://github.com/devsoap/gradle-vaadin-flow.git
```

The project is a Gradle project so you can just import it into your preferred IDE.

The plugin is written in the Groovy programming language.

#### Source structure

The project has 3 source folders.

The **src/main** source folder contains the sources of the plugin. This is most likely where you will be working.

The **src/test** source folders contains unit tests. 

The third source folder, **src/functionalTest** is for integration/functional tests. These tests are run using the Gradle Test Runner so they are run using an actual Gradle installation.  The tests are written in Spock and Groovy.

#### Plugin package structure

The source code is split into 6 different sub packages:

##### actions

Every action is related to a plugin that is applied to the project. For example, the GrettyPluginAction is only run for 
projects which have applied the Gretty plugin. The purpose of these actions is to configure third party plugins so they
integrate in a seamless manner with the Vaadin Flow plugin.

##### creators

Creators are responsible for creating files and folders for the user from pre-defined templates. They are mostly used for
the *vaadinCreate\** tasks. All creator tasks operates on a model.

##### models

Models provide the data for the creator tasks. They do not contain any business logic and are only meant for conveying 
data from one place to another.

##### extensions

Extensions extend the project with configuration options. They provide the way users of the plugin configures the plugin
to behave as they need.

##### tasks

Tasks provide the run targets for the Gradle project. All tasks should start with *vaadin* and should be grouped in
a *vaadin* group.

##### util

Util contains various utilities for perform file and class traversing tasks.

#### Building the plugin

The plugin can be built by running the **jar** task. After a successful build the plugin artifact will be placed in the
``build/libs`` folder.

#### Using the plugin from a test project


The easiest way to test your changes is to create another project where you just apply the plugin. It can be an existing 
Vaadin project that already uses the gradle plugin or a new one.

The project comes with an apply script (local.plugin) that you can use to apply the built project. 

In your test project replace any existing plugin with 

```groovy
apply from: '<path to this project>/local.plugin'
```

Here is an example of a complete ``build.gradle`` using this approach:

```groovy
// Apply all other plugin as you normally would
plugins {
  id "org.gretty" version '2.3.1'
  id 'java'
}

// Build the plugin jar and use the applyscript to apply it
apply from: '<path yo your plugin repository>/gradle-vaadin-flow/local.plugin'

// ... Continue configuring the project as you normally would
vaadin.autoconfigure()
```

You might need to tweak the paths in ``local.plugin`` to suit your environment. By default it uses 
``$HOME/Repositories/gradle-vaadin-flow/build/libs`` to locate the plugin, but if you have a different path you will
need to change it. Please don't submit these changes however in a pull request, the CI server requires this path.
 

#### Running the function/integration tests 

The integration tests are normal JUnit tests leveraging the Spock framework.

If you only want to run a single test then it can be achieved by running the following:

```bash
./gradlew -Dorg.gradle.testkit.debug=true functionalTest -x test --stacktrace --info --tests "*<name of test here>*"
```

If you have a PRO subscription you can also add the following environment variables to be able to run the test in PRO mode:

```
DEVSOAP_EMAIL=<PRO subscription email>
DEVSOAP_KEY=<PRO subscription key>
```

##### Pull requests

When you submit a pull request you will need to ensure two things are set:

> Sign the CLA

The CLA can easily be signed by just clicking on the link after you have submitted the pull request. 

> Ensure all functional/integration tests are green

Once you submit the pull request Travis (CI) will run all the tests. If some of the fail then please fix them and try
again. 

It is encouraged to use git rebase when pushing changes to the pull request branches as that will keep the Git history
clean once the pull request is merged.
