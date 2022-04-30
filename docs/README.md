# TransektCount

TransektCount is an Android app that supports transect counters in nature preserving projects according to the European Butterfly Monitoring Scheme methodology. 
It allows a species-specific counting per transect section. 

### Features

- Data recording is organized according to a single transect inspection. 
That means, you will use a new database instance per inspection by importing a prepared basic Database.
- Databases can be individually created and adapted within the app regarding meta data, transect sections and expected butterfly species.
- You may use an individual species list per transect section.
- It contains a prepared basic database with some of the expected species as a starting point.
- Editor for the transect sections to setup or adapt its species
- Editor for meta data to prepare an inspection
- Editor for species remark or bulk count entry
- Counting page with scroll menu to select species
- Count input per internal/external counting area and here each with separate counters for ♂♀, ♂, ♀, pupa, larva or egg.
- Results are prepared for easy readout to transfer them to science4you entry masks.
- Results are shown with meta data, remarks, totals and species results per section.
- Results may also be exported in SQLite- or CSV-format and transferred to a PC for your own processing, e.g. by importing a csv-file into a spreadsheet like MS Excel.
- The integrated help contains a detailed userguide.

### Prerequisites
The app demands for 
- storage access permit which is needed for im-/exporting the counting data, 
- the permit to keep the device awake.

The smartphone screen should have a minimum resolution of 1920 x 1024 pixels. 
The app is usable with Android version 4.4 (KitKat) or newer.

### General
#### Documentation
Documentation is provided under  
https://github.com/wistein/TransektCount/tree/master/docs.

Please read the userguide (provided in German and English) or consult its contents in the app's help 
before using the app.  
There are further documents e.g. for setting up and using the app as well as sample basic databases 
(transektcount0.db, transektcount0_Ab01.db and respective English versions) provided under /docs.

The development history is listed in History.md in /docs.

Source file structure is for compilation by Android Studio.

#### Availability
The app is available on F-Droid under  
https://f-droid.org/packages/com.wmstein.transektcount/.

The apk-file 'transektcount-release.apk' is also provided in the GitHub repository under  
https://github.com/wistein/TransektCount/tree/master/apk.

Please note that both versions are compiled from the same sources but signed differently and so cannot 
be mutually updated.

### License:

Copyright 2016-2022 Wilhelm Stein (wistein)

TransektCount is licensed under the Apache License, Version 2.0 (the "License");
you may not use any of its files except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Integrated code from external references:
- Counting functions based on BeeCount, (C) 2016 Milo Thurston (knirirr), 
  Apache License, Version 2.0, https://github.com/knirirr/BeeCount
- AutoFitText.java, modified, original code from author: pheuschk, 18.04.2013, 
  Open Source, https://pastebin.com/raw/e6WyrwSN
- ChangeLog.java, modified, original code (C) 2011-2013, Karsten Priegnitz, 
  Other Open Source, https://github.com/koem/android-change-log/
- CSVWriter.java, modified, original code (C) 2015 Bytecode Pty Ltd., 
  Apache License, Version 2.0
- filechooser based on android-file-chooser, 2011, Google Code Archiv, GNU GPL v3.

Butterfly pictures: 
- p06243.png derived from https://en.wikipedia.org/wiki/File:Crambus.pascuella.7563.jpg, Olaf Leillinger, GNU Free Documentation License
- p06364.png derived from https://upload.wikimedia.org/wikipedia/commons/f/f6/%281323%29_Pediasia_contaminella_%2833480878804%29.jpg, Ben Sale, Creative Commons Attribution 2.0 Generic License
- p06499.png derived from https://upload.wikimedia.org/wikipedia/commons/3/37/Evergestis_extimalis1.JPG, Adam Furlepa, CC BY-SA 3.0
- p06855.png derived from https://tpittaway.tripod.com/sphinx/h_gal_a3.jpg (non-commercial use allowed)
- p06884.png derived from https://en.wikipedia.org/wiki/Carcharodus_lavatherae, Dumi, GNU Free Documentation License, version 1.2

- all other pictures, app icon and background (C) Wilhelm Stein. 

Pictures of this work may be used for research or other non-commercial purposes without prior permission. Please note that illustrations provided by other authors remain the copyright of those authors and should not be reproduced other than with their permission.
