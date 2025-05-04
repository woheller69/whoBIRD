<pre>Send a coffee to 
woheller69@t-online.de 
<a href= "https://www.paypal.com/signin"><img  align="left" src="https://www.paypalobjects.com/webstatic/de_DE/i/de-pp-logo-150px.png"></a>

  
Or via this link (with fees)
<a href="https://www.paypal.com/donate?hosted_button_id=XVXQ54LBLZ4AA"><img  align="left" src="https://img.shields.io/badge/Donate%20with%20Debit%20or%20Credit%20Card-002991?style=plastic"></a></pre>


# whoBIRD - Identify bird sounds in real time

[<img src="preview.jpeg" height="255"/>](https://www.youtube.com/embed/YYML_-e3yls) <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/01.png" width="150"/> <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/02.png" width="150"/> <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/03.png" width="150"/>

Introducing whoBIRD, the ultimate birding companion that can recognize birds by their sounds, anywhere in the world!
Powered by the cutting-edge BirdNET project, whoBIRD boasts an extensive database of over 6,000 bird species worldwide.
Using advanced machine learning algorithms, this Android app can accurately identify birds based on their unique vocalizations.

What's more, whoBIRD performs its magic in real time entirely on your device, without requiring an internet connection.
This means you can use it anytime, anywhere – whether you're deep in the forest or at the edge of a remote lake.

<a href="https://f-droid.org/packages/org.woheller69.whobird/" target="_blank">
<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" alt="Get it on F-Droid" height="80"/></a>

# Instructions
## Getting Started

At first start the app will download the required BirdNET model files.
Once the app is installed simply open it and it will begin listening and analyzing.

## Detection Notifications

If a bird is detected, its name will be briefly displayed. For a detailed list of all detections, navigate to the View tab.
There you can also backup, share, or delete the database containing your observations.

## Customization Options

**Ignore Date and Place**: Disable the meta model that checks if a bird can be present at your location at the current time. Useful when analyzing recordings from other locations.

**Show Images**: When enabled, an image of the detected bird will be downloaded if the detection probability is high.

**Audio Source**: Select the audio input that works best for your device. Typically, "Unprocessed" is the recommended choice. If using a USB microphone, select "Microphone".

**High Pass Filter**: Filter out low frequencies to reduce background noise, such as traffic sounds. For example, a 200Hz filter can help minimize low-frequency noise.

**Threshold**: Set the minimum probability required for a detection to be displayed. Be cautious when lowering the threshold, as it may lead to an increase in false detections.

**Weighted Meta Model**: In addition to running the meta model for the current week and location, this version computes the meta model predictions for all weeks of the year at the same location. 
The final result is a 50/50 weighted average of:
-The prediction for the current week, and
-The maximum prediction across all weeks.

This approach improves detection of migratory birds, especially when they arrive earlier or leave later than the typical migration period.

**Save .wav files**: Save a .wav file for each detection in Music directory. Recordings in this directory are not deleted by the app. Make sure to clean up on your own. Requires Android 12+.

WARNING: This option may consume a lot of space in storage.

**Language** On Android 13+ you can set the app language in Android app settings.

## Contribute

For translations use https://toolate.othing.xyz/projects/whobird/

# License
This work is licensed under GPLv3, © woheller69

- This app is built on the [BirdNET framework](https://github.com/kahst/BirdNET-Analyzer) by [**@kahst**](https://github.com/kahst), published under CC BY NC SA 4.0 license
- At first start it downloads the BirdNet TFLite library from [whoBird-TFlite](https://github.com/woheller69/whoBIRD-TFlite), which is published under CC BY NC SA 4.0 license
- Label files from BirdNET are used under GPL 3.0 with [permission from the author](https://github.com/woheller69/whoBIRD/issues/1)
- It uses code from [Tensorflow](https://www.tensorflow.org/lite/examples) examples, published under [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0.html)
- It uses Zip4j (https://github.com/srikanth-lingala/zip4j) which is licensed under Apache License Version 2.0
- It uses iirj (https://github.com/berndporr/iirj) which is licensed under Apache License Version 2.0

# OTHER APPS

| **RadarWeather** | **Gas Prices** | **Smart Eggtimer** |
|:---:|:---:|:---:|
| [<img src="https://github.com/woheller69/weather/blob/main/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.weather/) | [<img src="https://github.com/woheller69/spritpreise/blob/main/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.spritpreise/) | [<img src="https://github.com/woheller69/eggtimer/blob/main/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.eggtimer/) |
| **Bubble** | **hEARtest** | **GPS Cockpit** |
| [<img src="https://github.com/woheller69/Level/blob/master/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.level/) | [<img src="https://github.com/woheller69/audiometry/blob/new/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.audiometry/) | [<img src="https://github.com/woheller69/gpscockpit/blob/master/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.gpscockpit/) |
| **Audio Analyzer** | **LavSeeker** | **TimeLapseCam** |
| [<img src="https://github.com/woheller69/audio-analyzer-for-android/blob/master/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.audio_analyzer_for_android/) |[<img src="https://github.com/woheller69/lavatories/blob/master/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.lavatories/) | [<img src="https://github.com/woheller69/TimeLapseCamera/blob/master/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.TimeLapseCam/) |
| **Arity** | **Cirrus** | **solXpect** |
| [<img src="https://github.com/woheller69/arity/blob/master/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.arity/) | [<img src="https://github.com/woheller69/omweather/blob/master/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.omweather/) | [<img src="https://github.com/woheller69/solXpect/blob/main/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.solxpect/) |
| **gptAssist** | **dumpSeeker** | **huggingAssist** |
| [<img src="https://github.com/woheller69/gptassist/blob/master/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.gptassist/) | [<img src="https://github.com/woheller69/dumpseeker/blob/main/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.dumpseeker/) | [<img src="https://github.com/woheller69/huggingassist/blob/master/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.hugassist/) |
| **FREE Browser** | **whoBIRD** | **PeakOrama** |
| [<img src="https://github.com/woheller69/browser/blob/newmaster/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.browser/) | [<img src="https://github.com/woheller69/whoBIRD/blob/master/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.whobird/) | [<img src="https://github.com/woheller69/PeakOrama/blob/master/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.PeakOrama/) |
| **Whisper** | **Seamless** | **SherpaTTS** |
| [<img src="https://github.com/woheller69/whisperIME/blob/master/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.whisper/) | [<img src="https://github.com/woheller69/seamless/blob/master/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.seemless/) | [<img src="https://github.com/woheller69/ttsengine/blob/master/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.ttsengine/) |

