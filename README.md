# MyLocation
Know your geo coordinates using on-device GPS and Network location providers

<a href="https://f-droid.org/packages/com.mirfatif.mylocation"><img alt="Get it on F-Droid" src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" height="80"></a>
<a href="https://play.google.com/store/apps/details?id=com.mirfatif.mylocation.ps"><img alt="Get it on Google Play" src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" height="80"></a>
<a href="https://apt.izzysoft.de/fdroid/index/apk/com.mirfatif.mylocation"><img alt="Get it on F-Droid" src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png" height="80"></a>

[![Github](https://img.shields.io/github/v/release/mirfatif/MyLocation?label="Github")](https://github.com/mirfatif/MyLocation/releases/latest) [![F-Droid](https://img.shields.io/f-droid/v/com.mirfatif.mylocation.svg?label="F-Droid")](https://f-droid.org/packages/com.mirfatif.mylocation) [![IzzyOnDroid](https://img.shields.io/endpoint?url=https://apt.izzysoft.de/fdroid/api/v1/shield/com.mirfatif.mylocation)](https://apt.izzysoft.de/fdroid/index/apk/com.mirfatif.mylocation) [![Telegram](https://img.shields.io/badge/Telegram-latest-blue)](https://t.me/mirfatifApps)

<sup>* Join [Telegram support group](https://t.me/MyLocationApp) to get instant updates and test beta releases.</sup>

## Features

My Location finds your device's location in the following ways:

* <b>GPS</b> is usually the most accurate method. But a position fix may take some time or may not work at all due to signal loss. <b>Lock GPS</b> feature runs a persistent service to keep connected with the satellites.

  You can also see the list of visible **satellites** with their PRNs (unique identifiers) and SNR (signal quality). Pro version shows extra telemetry including satellite name, angles and frequency bands.
 
* <b>Network Location Provider</b> uses Wi-Fi or Cellular ids to estimate the location. On the devices with Google Play Services installed, NLP usually uses Google Location Service at backend.
* <b>UnifiedNLP</b> is an open source API which has been used to develop multiple [NLP backends](https://github.com/microg/UnifiedNlp/wiki/Backends).

Furthermore:

* Location coordinates can be copied to clipboard or opened in a maps app, if installed.
* Pro version has a `-map` variant which includes a built-in **map preview** using OpenStreetMap.
* Pro version also has a minimal **compass**.
* Clearing A-GPS aiding data is also supported.

<b>Note</b> that My Location is not a location provider like UnifiedNLP (or microG GmsCore). It just shows the location information received from AOSP location providers (GPS and Network) or directly from UnifiedNLP Backends (whichever are available on the device).

## Screenshots

<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.jpg" width="250"> <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.jpg" width="250"> <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.jpg" width="250">

<img src="screenshots/1.webp" width="250"> <img src="screenshots/2.webp" width="250"> <img src="screenshots/3.webp" width="250"> <img src="screenshots/4.webp" width="250">

## Translation
[![Crowdin](https://badges.crowdin.net/my-location/localized.svg)](https://crowdin.com/project/my-location)

## Third-Party Resources

Credits and thanks to the developers of:
* [Android Jetpack](https://github.com/androidx/androidx)
* [Material Components for Android](https://github.com/material-components/material-components-android)
* [Unified NLP](https://github.com/microg/android_external_UnifiedNlpApi)
* [Spotless GoogleJavaFormat](https://github.com/diffplug/spotless)
* [MapLibre Compose](https://github.com/maplibre/maplibre-compose) + [OpenFreeMap](https://openfreemap.org/) + [OpenStreetMap](https://www.openstreetmap.org)
* [LeakCanary](https://github.com/square/leakcanary)

## License [![License](https://img.shields.io/github/license/mirfatif/MyLocation?label="License")](https://github.com/mirfatif/MyLocation/blob/master/LICENSE)

You **CANNOT** use and distribute the app icon in anyway, except for **My Location** (`com.mirfatif.mylocation`) app.

    My Location is free software: you can redistribute it and/or modify
    it under the terms of the Affero GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    Affero GNU General Public License for more details.

    You should have received a copy of the Affero GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.

## Want To Reach Us?

* [Telegram Group](https://t.me/MyLocationApp)
* [XDA Forums Thread](https://forum.xda-developers.com/t/app-5-0-my-location-know-your-geo-coordinates.4306185)
* Email: mirfatif dot dev at gmail dot com
