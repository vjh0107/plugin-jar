# PluginJar
![Maven Central Version](https://img.shields.io/maven-central/v/kr.junhyung/pluginjar-plugin)
![GitHub License](https://img.shields.io/github/license/vjh0107/plugin-jar)

PluginJar is a Gradle plugin designed to simplify the creation of plugin.yml files for Minecraft plugins.

By using this plugin, there is no need to use ShadowJar to merge projects in a Gradle multi-project environment.
This plugin is specifically designed to avoid the creation of fatjars in Minecraft plugin development.

## Getting Started

1. Add the following to your `build.gradle.kts` file.
```kotlin
plugins {
    id("kr.junhyung.pluginjar") version "0.0.3"
}
```
2. Annotate your main plugin class with @Plugin annotation.
```java
@Plugin
public class ExamplePlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        System.out.println("Hello, PluginJar!");
    }
}
```
3. Build your project by running the following task.

```
 ./gradlew pluginJar
```

## Property sources

| Property   | Source                       |
|------------|------------------------------|
| name | Root project name. |
| main | Fully qualified name of the class with @Plugin annotation, auto-detected.  |
| version | Project version.  |
| api-version | Automatically determined from Bukkit API implementation dependency in the compileClasspath.    |
| description | Project description.  |
| libraries | Dependencies collected from the runtimeClasspath. If the project depends on other projects at runtime, their dependencies are also included. |

## License

Licensed under the Apache License, Version 2.0
