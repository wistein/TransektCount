# TransektCount

TransektCount is an Android app that supports transect counters in nature preserving projects according to the Butterfly Monitoring Scheme methodology. 
It allows a species-specific counting per transect section. 

The integrated database is organized according to a single transect inspection. That means, a new (prepared and importable) basic database instance will be used per inspection.

Databases can be individually created and adapted within the app regarding meta data, transect sections and expected butterfly species.
The recorded data results (meta data, counts and remarks) may either be read on the smartphone or exported in SQLite- or CSV-format and transferred to a PC for your own processing, e.g. by importing a csv-file into MS Excel.

The app demands for storage access permits which are needed for im-/exporting the counting data, the permit to write settings (to set screen brightness) and the permit to prevent the phone from sleeping (to control the counting screen when used under Android 5.0.1 or newer). 

Before using the app, please, read the documentation (provided in German and English).
There are a detailed description for setting up and using the app as well as sample basic databases (transektcount0.db, transektcount0_Ab01.db and respective English versions) provided under https://github.com/wistein/TransektCount/tree/master/transektcount/docs.

The app is available on F-Droid. The apk-file 'transektcount-release.apk' is also provided under https://github.com/wistein/TransektCount/tree/master/transektcount/apk.
Please note that both versions are compiled from the same sources but signed differently and so cannot be mutually updated.

It is usable with Android version 4.2.2 or newer.

Source file structure is for compilation by Android Studio.

### License:

Copyright 2016-2017 Wilhelm Stein (wistein)

TransektCount is licensed under the Apache License, Version 2.0 (the "License");
you may not use any of its files except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

App icon and app background picture: wistein

All butterfly pictures: wistein
