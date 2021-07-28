![ic_launcher_round](https://user-images.githubusercontent.com/55358113/122177060-8e66b500-ce85-11eb-89d4-ee1b7636bf12.png)

# NextPass
NextPass is an Android client for Passwords (https://git.mdns.eu/nextcloud/passwords), a simple yet feature-rich password manager for Nextcloud.<br />NextPass is still in development and will receive further updates. **Icon is temporary**.<br />

# Screenshots 
(taken in version 1.6.0)
![NextPass-screenshots-collage(2)](https://user-images.githubusercontent.com/55358113/124677906-b4e6a180-dec1-11eb-9745-88bcd88c9071.jpg)


# Build and run
Android Studio >=Arctic Fox and Kotlin >=1.4.32 are **required** to build the app without issues. Just import the project from Version Control, let Gradle do its (endless) magic and then build&run. **Do not change gradle and proguard configuration**.<br />Note that Passwords (https://apps.nextcloud.com/apps/passwords) needs to be installed on your server.

# Technologies
NextPass is built using only the latest and greatest. It is 100% Kotlin and a single-activity app. Starting from version 2.0.0, NextPass does not use third party Java libraries anymore. As of now, NextPass uses Ktor library to make network requests, with its http engine CIO to guarantee concurrency, and Kotlinx-serialization library to manage json responses.<br />NextPass also relies on Jetpack Compose to build the UI. These technologies project NextPass into the future (or just the present, perhaps).


# Requirements
NextPass officially supports Android up from 8.0. Please, if you experience issues, report them on GitHub providing a detailed description (you can do this just by opening 'About' screen in the app itself). NextPass can only work with an internet connection.

# Known issues
**Jetpack Compose**<br />
Compose is still in beta, that means that you could experience some glitches and/or poor performance depending on your device. NextPass UI also lacks many animations, due to Compose navigation and foundation libraries still not supporting them. I will update the toolkit ASAP to provide the best possible experience.

**Autofill service**<br />
Autofill service provides suggestions when you're logging in websites and apps. It should work properly most of the times, however sometimes it can seem partially or completely broken. This is due to the fact that the service requires some details and info to recognize the input fields, and apps/websites should provide them. If they don't, then NextPass just can't do its magic.
Also, even if NextPass manages to recognize the input fields, suggestions may not be found, despite they're on your service. This happens because NextPass has a certain logic to follow to pick the correct suggestions. For instance, as long as the password's label matches (at least partially) the packagename of the app you're logging into, then everything's alright. Passwords saved with NextPass' autofill service will be created with a custom field containing the full packagename of the app; this will guarantee that the new password is always found by autofill service (as long as it finds input fields too).
If you discover that autofill doesn't work on one particular app, just open an issue and I'll try to help.


# Translations
Currently, NextPass only supports english, but translations are so very welcome. I plan to setup an actual translation platform (Crowdin, probably) in the future, but, until then, you can open an issue specifying your language and I will provide anything you need. Also, I'm not english native, so if you have suggestions for the current english translation, please report.

# Special thanks and mentions
Go to: https://github.com/seldon1000/NextPass/blob/master/THANKS.md
