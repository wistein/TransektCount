# TransektCount

TransektCount is an Android app that supports transect counters in nature preserving projects according to the European Butterfly Monitoring Scheme methodology. 
It allows a species-specific counting per transect section. 

### Features
- Data recording is organized according to a single transect inspection. 
That means, you will use a fresh database instance per inspection by clearing the current database or importing a prepared basic database.
- It contains a prepared basic database with some of the expected species as a starting point.
- Databases can be individually created and adapted within the app regarding meta data, transect sections and expected butterfly species.
- Transect sections can be selected manually (automatically via GPS has been abandoned due to faulty performance).
- There are editors 
  - for the transect counting list to setup or adapt its species,
  - for meta data to prepare an inspection and
  - for species remark or bulk count entry.
- The counting page has a scroll-list to select a species by picture and name.
- Counts are recorded per internal/external counting area and here each with separate counters for ♂|♀, ♂, ♀, pupa, larva or egg.
- Results are prepared for easy readout to transfer them to the entry masks of Tagfalter-Monitoring Deutschland or similar butterfly monitoring sites.
- Results are shown with meta data, remarks, totals and species results per section.
- Results may also be exported in SQLite- or CSV-format and transferred to a PC for your own processing, e.g. by importing a csv-file into a spreadsheet.
- The current butterfly list can be exported for later re-import or imported by the complementary app TourCount.
- The integrated help contains a detailed user guide.
- TransektCount uses the codes of the European coding scheme for butterflies from Karsholt/Razowski.

### Prerequisites
The app demands for 
- storage access permit which is needed for im-/exporting the counting data and 
- the permit to keep the device awake.

The device screen should have a minimum resolution of 1920 x 1024 pixels. 
The app is usable with Android version 7.1 (Nougat) or newer.

### General
#### Documentation
Documentation and example DBs are provided under  
https://github.com/wistein/TransektCount/tree/master/docs.

Please read the user guide (provided in German and English) or consult its contents in the app's help 
before using the app.  
There are further documents e.g. for setting up and using the app as well as sample basic databases 
(transektcount0_xxxx.db or species lists in German and English versions) provided under .../docs.

The development history is listed in History.md in .../docs.

Source file structure is for compilation by Android Studio.

#### Availability
The app is available on F-Droid under  
https://f-droid.org/packages/com.wmstein.transektcount/.

All versions of the apk-file 'transektcount-release_nnn.apk' are provided in the GitHub repository under 
https://github.com/wistein/TransektCount.

Please note that both versions are compiled from the same sources but signed differently and so cannot 
be mutually updated.

On GitHub you will find the published stable version when clicking the "master"-button and select 
the Tag with the highest Branch number n.n.n. Then get the file transektcount-release_nnn.apk under apk. 

The latest development version of 'transektcount-release_nnn.apk' is also provided in the GitHub 
repository under the master branch https://github.com/wistein/TransektCount/tree/master/apk.

### Licenses:
Copyright 2016-2025 Wilhelm Stein

TransektCount is licensed under the Apache License, Version 2.0 (the "License");
you may not use any of its files except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Pictures of this work by Wilhelm Stein may only be used for scientific or non-commercial purposes.

Code and illustrations provided by other authors remain the copyright of those authors and should not be reproduced or distributed other than with their license.

For detailed copyrights on external code and pictures see the separate document "License.txt" in TransektCount/docs.
