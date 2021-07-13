![ic_launcher_round](https://user-images.githubusercontent.com/55358113/122177060-8e66b500-ce85-11eb-89d4-ee1b7636bf12.png)

# NextPass
NextPass is an Android client for Passwords (https://git.mdns.eu/nextcloud/passwords), a simple yet feature-rich password manager for Nextcloud.<br />NextPass is still in development and will receive further updates. **Icon is temporary**.<br />

# Screenshots 
(taken in version 1.6.0)
![NextPass-screenshots-collage(2)](https://user-images.githubusercontent.com/55358113/124677906-b4e6a180-dec1-11eb-9745-88bcd88c9071.jpg)


# Build and run
Android Studio Arctic Fox (currently Beta branch) or Bumblebee (currently Canary branch, recommended) and Kotlin >=1.4.32 are **required** to build the app without issues. Just import the project from Version Control, let Gradle do its (endless) magic and then build&run. **Do not change gradle and proguard configuration**.<br />Note that Passwords (https://apps.nextcloud.com/apps/passwords) needs to be installed on your server.

# Technologies
NextPass is built using only the latest and greatest. It is 100% Kotlin and a single-activity app. Starting from version 2.0.0, NextPass does not use third party Java libraries anymore. As of now, NextPass uses Ktor library to make network requests, with its http engine CIO to garantee concurrency, and Kotlinx-serialization library to manage json responses.<br />NextPass also relies on Jetpack Compose to build the UI. These technologies project NextPass into the future (or just the present, perhaps).


# Requirements
NextPass officially supports Android up from 8.0. Please, if you experience issues, report them on GitHub providing a detailed description (you can do this just by opening 'About' screen in the app itself).


# Known issues
**Jetpack Compose**<br />
Compose is still in beta, that means that you could experience some glitches and/or poor performance depending on your device. NextPass UI also lacks many animations, due to Compose's navigation and foundation libraries still not supporting them. I will update the toolkit ASAP to provide the best possible experience.


# Translations
Currently, NextPass only supports english, but translations are so very welcome. I plan to setup an actual translation platform (Crowdin, probably) in the future, but, until then, you can open an issue specifying your language and I will provide anything you need. Also, I'm not english native, so if you have suggestions for the current english translation, please report.

# Special thanks and mentions
Go to: https://github.com/seldon1000/NextPass/blob/master/THANKS.md


***README IN PROGRESS***
