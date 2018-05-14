# EEG Data Collection  
An Android application that aims to collect Stress and Mindfulness state as training data to generate a SVM Model for Active/Mindfulness State prediction.  

Stressor
- The Montreal Imaging Stress Task.  

Mindfulness
- Guided Meditation from The Honest Guys (https://www.youtube.com/watch?v=6p_yaNFSYao&t=519s)

# Installation Guide

## Software Installed and Tested On
- Android Studio 3.1.2
- Windows 10 Version 1803

## Hardware used
- LG G6 (Android 7.0) Build Number: NRD90U

## Gradle dependencies
```
implementation 'com.android.support:support-v4:26.1.0'
implementation files('libs/libmuse_android.jar')
implementation files('libs/bsh-core-2.0b4.jar')
implementation 'com.android.support.constraint:constraint-layout:1.0.2'
```

## Setup Guide

Install Android studio from https://developer.android.com/studio/

1. Clone Repository using Android Studio
![alt text](https://github.com/zhijiee/FocusDataCollection/blob/master/setup_guide/1.PNG)
2. Open the project 
![alt text](https://github.com/zhijiee/FocusDataCollection/blob/master/setup_guide/2.PNG)
3. Import Project from Gradle with default settings. 
![alt text](https://github.com/zhijiee/FocusDataCollection/blob/master/setup_guide/3.PNG)
4. Let android studio load the project. It will prompt you to install 'android-26'. If not installed click on 'install missing platform and sync project'.   
![alt text](https://github.com/zhijiee/FocusDataCollection/blob/master/setup_guide/4.PNG)
5. Click on next to install Android SDK Platform 26
![alt text](https://github.com/zhijiee/FocusDataCollection/blob/master/setup_guide/5.PNG)

![alt text](https://github.com/zhijiee/FocusDataCollection/blob/master/setup_guide/6.PNG)
6. Once completed, you are now able to build and run app. Press Shift + F10 to run the application. You may get a prompt asking you to update Gradle Plugin and install instant run. You can choose to install or not. Both will work. 
![alt text](https://github.com/zhijiee/FocusDataCollection/blob/master/setup_guide/7.PNG)
![alt text](https://github.com/zhijiee/FocusDataCollection/blob/master/setup_guide/8.PNG)

