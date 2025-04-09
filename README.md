# Android Recorder API Godot Plugin
This repository provides the basic functionality of the step counter from the Recording API for Godot 4.4.

## Contents
* A simple plugin for the Android Recording API to use in Godot: [`plugin/addons`](plugin/addons)
* Source code for modifying and implementing new features [`plugin/src/main/java/com/lachlanroberts/plugin/stepcounter/`](plugin/src/main/java/com/lachlanroberts/plugin/stepcounter/)

## Usage
**Note:** [Android Studio](https://developer.android.com/studio) is the recommended IDE for
developing Godot Android plugins. 
You can install the latest version from https://developer.android.com/studio.


### Building the configured Android plugin
- In a terminal window, navigate to the project's root directory and run the following command:
```
./gradlew assemble
```
- On successful completion of the build, the output files can be found in
  [`plugin/addons`](plugin/addons)

### Running the Android plugin
- Open your project in Godot (4.2 or higher)
- Extract the plugin into your project (e.g. /addons/StepCounterAndroidPlugin)
- Navigate to `Project` -> `Project Settings...` -> `Plugins`, and ensure the plugin is enabled
- Install the Godot Android build template by clicking on `Project` -> `Install Android Build Template...`
- Update your Android export preset min SDK to `26` or higher by clicking on `Project` -> `Export...` -> `Min SDK`
- Update your Godot script to reference the plugin (see example below)
- Connect an Android device to your machine and run the demo on it

##### Example code for calling the plugin

This example shows how to set up and then call the plugin function

```
var _plugin_name = "StepCounterAndroidPlugin"
var android_step_plugin = null

func _ready():
  # Set up Android Plugin for Steps
  if Engine.has_singleton(_plugin_name):
      android_step_plugin = Engine.get_singleton(_plugin_name)
      # Check the plugin has access to the Recording API, if not request it
      if android_step_plugin.checkRequiredPermissions() != 0:
          android_step_plugin.requestRequiredPermissions()
      
      # Start recording steps
      android_step_plugin.subscibeToFitnessData()
      # Connect the signal for returning the number of steps
      android_step_plugin.total_steps_retrieved.connect(_total_steps_retrieved)
      
      # Check the number of steps after a minute
      await get_tree().create_timer(60).timeout
      print(get_steps_in_last_seconds(60))
  else:
      printerr("Couldn't find plugin " + _plugin_name)
      
func get_steps_in_last_seconds(numSeconds):
  # Async call to get the steps, returns results via total_steps_retrieved signal
  android_step_plugin.getStepsInLastSeconds(numSeconds)

func _total_steps_retrieved(total_steps):
  print(total_steps)
```

