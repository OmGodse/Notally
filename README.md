### Background
I started this as a side project back in October 2019. Although there are many notes apps out there, they're all hideous, glitchy, low quality or all 3 at the same time.

Maybe the developer views developing a beautiful user interface as the realm of _lowly_ designers. Maybe he just doesn't care.

Well, Notally is none of these things. It's extremely light, minimalistic and elegant. There are minimal dependencies and lines of code. (All without compromising on readability)

### Architecture
Notes and lists are stored as XML files in the app's internal directory under different folders. Labels are stored in the shared preferences.

The different directories containing notes are observed by FileObservers and relevant updates are dispatched via LiveData to update the UI.

Different screens in the app (Aside from the Take Note and Make List) are represented by fragments, managed by the Android Navigation Component.

### Features
* Auto save
* Dark mode
* Material design
* Create lists to stay on track
* Support for Lollipop devices and up
* Add labels to your notes for quick organisation
* Archive notes to keep them around, but out of your way
* Export notes as plain text, HTML or PDF files with formatting
* Create rich text notes with support for bold, italics, mono space and strike-through
* Add clickable links to notes with support for phone numbers, email addresses and web urls

### Downloads
[<img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" alt="Get it on Google Play"  height="70"/>](https://play.google.com/store/apps/details?id=com.omgodse.notally)
[<img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png" alt="Get it on IzzyOnDroid" height="70"/>](https://apt.izzysoft.de/fdroid/index/apk/com.omgodse.notally)

### Translations
* 🇬🇧 English
* 🇮🇩 Indonesian by [zmni](https://github.com/zmni)
* 🇮🇷 Italian by Luigi Sforza
* 🇪🇸 Spanish by Jose Casas
* 🇺🇦 Ukrainian by Alex Shpak
* 🇸🇪 Swedish by Erik Lindström
* 🇷🇺 Russian by Denis Bondarenko
* 🇫🇷 French by Arnaud Dieumegard
* 🇧🇷 Brazilian Portuguese by [fabianski7](https://github.com/fabianski7)
* 🇳🇴 Norwegian by Fredrik Magnussen
* 🇵🇭 Tagalog by Isaiah Collins Abetong
* 🇩🇪 German by Maximilian Braunschmied

If you would like to help translate the app, please contact me [here](mailto:omgodseapps@gmail.com)

### Screenshots
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width="250"/><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" width="250"/><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" width="250"/>