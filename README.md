![ic_launcher_round](https://user-images.githubusercontent.com/55358113/122177060-8e66b500-ce85-11eb-89d4-ee1b7636bf12.png)

# NextPass
NextPass is an Android client for Passwords (https://git.mdns.eu/nextcloud/passwords), a simple yet feature-rich password manager for Nextcloud.<br />NextPass is still in development and will receive further updates. **Icon is temporary**.<br />Check 'Know issues' section down below before using the app.



# Build and run
NextPass is built entirely using Jetpack Compose toolkit, hence Android Studio Arctic Fox (currently Beta branch) or Bumblebee (currently Canary branch, recommended, used to develop version 1.0) and Kotlin >=1.4.32 are **required** to build the app without issues.
Just import the project from Version Control, let Gradle do its (endless) magic and then build&run.<br />Nextcloud app is required by NextPass to work and communicate with the Nextcloud server: you can install it yourself from  the official GitHub repo, Play Store or FDroid, or you can just open NextPass and you will be redirected to Nextcloud's Play Store page at some point.<br />Note that Passwords (https://apps.nextcloud.com/apps/passwords) needs to be installed.


# Requirements
NextPass officially supports Android up from 8.0, although pre-11 versions could experience some weird behavior. Please, if you experience issues with a less recent Android version, report them on GitHub providing a detailed description (you can do this just by opening 'About' screen in the app itself).


# Known issues
**Jetpack Compose**<br />
Compose is still in beta, that means that you could experience some glitches and/or poor performance depending on your device. NextPass UI also lacks many animations, due to Compose's navigation and foundation libraries still not supporting them. I will update the toolkit ASAP to provide the best possible experience.


**Password edit**<br />
Editing passwords is pissible in NextPass, although it's not perfect. Instead of directly updating a password with the new informations, NextPass generates a totally new password and then deletes the old one. That leads to several (kinda) issues: the password's ID, which should be immutable, changes; the old password is moved to 'Trash', which is confusing; the actual password's creation date is updated to the current date, again, confusing. Apart from this, editing passwords works exactly as intended.

This is because NextPass communicates with the Nextcloud server through Nextcloud's official SingleSignOn library (https://github.com/nextcloud/Android-SingleSignOn). As of now, SSO does not support PATCH http method, which is required to perform the update action on the passwords. NextPass' workaround is temporary and will stay that way until either SSO provides full PATCH method support (for now, some talks are being made here http://github.com/nextcloud/Android-SingleSignOn/issues/355) or I switch to a different library to perform http requests.


# Translations
Currently, NextPass only supports english, but translations are so very welcome. I plan to setup an actual traslation platform (Crowdin, probably) in the future, but, until then, you can open an issue specifying your language and I will provide anything you need.


***README IN PROGRESS***
