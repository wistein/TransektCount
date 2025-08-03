# History

#### TransektCount Copyright © 2016-2025, Wilhelm Stein, Bonn, Germany

#### Version 4.2.8 (2025-08-03)
- Some code and text improvements
- Bugfixes

#### Version 4.2.7 (2025-07-20)
- Compiled for Android 16 (SDK 36)
- Help and License info with new design
- App info with background adjusted to new design
- Adaptive app icon added for Android 16 (prevents white ring)
- Some code and layout improvements
- Pictures of Ennomos quercinaria and Eilema lurideola added
- Docs updated
- Bugfixes

#### Version 4.2.6 (2025-07-06)
- Vibrator will only be used if Android Version is >= 8
- Code adapted for Android Version 15 for now forced edge-to-edge layout
- Some page layouts adapted for edge-to-edge layout
- Counting page layout better adapted for different screen heights
- Species editing page now uses left-handers mode
- All Snackbars (pop-up notifications) changed to colored Toasts
- Use dark navigation bar for TransektCount when System setting "Dark theme" is off
- Filename of exported species list with language indicator ('de' or 'en')
- Some code cleaning
- Docs updated

#### Version 4.2.5 (2025-05-18)
- Option to set the proximity Wakelock sensitivity (if supported by device)
- Corrected message when there is no suitable file for import
- Optional button vibration fixed for Android Version > 11  
- Code, data and layout improvements
- Some minor bugfixes
- Docs updated

#### Version 4.2.4 (2025-04-18)
- Counting page layout better adapted to screen size
- Export to a Basic DB now writes the transect No. into the filename
- Import of a Basic DB with file selection
- Exported results for sorted sections show time of 1. count per section
- Some text updates and corrections
- Docs updated

#### Version 4.2.3 (2025-03-25)
- Export of the current species list for import by TourCount and TransektCount
- Some text updates and corrections
- Bugfix to show correct transect No. after DB import
- Docs updated

#### Version 4.2.2 (2025-03-17)
- Added pictures of Yponameuta melinellus, Agriphila geniculea, Phigalia pilosaria, Idaea rusticata, Xanthorhoe fluctuata, Eupithecia vulgata, Eileme caniola
- Added export of the current species list for import in TourCount
- Added import of an exported TourCount species list
- Update Gradle -> 8.11.1
- Update AGP dependency -> 8.9.0
- Update kotlin -> 2.1.0
- Docs updated

#### Version 4.2.1 (2025-02-23)
- Backpressed logic on starting page improved
- Bugfix for starting the default camera if it is the only camera app
- Permissions handling improved
- Code improvements
- Update Gradle -> 8.10.2
- Update AGP dependency -> 8.8.1
- Docs updated

#### Version 4.2.0 (2024-12-17)
- csv output table modified
- Filenames of exported current DB and csv table contain transect No.
- App gets completely removed from memory when finished
- Options to preselect species by initial letters for adding, removing and editing them
- Code improvements and Kotlin code adapted to Kotlin 2
- Bugfixes
- Update Gradle -> 8.9
- Update AGP dependency -> 8.7.3
- Docs updated

#### Version 4.1.0 (2024-08-26)
- Functionality of GPS-based selection of section removed as too unreliable
- File selection view enhanced with headline
- Preferences view enhanced with headline
- Preferences show their states in menu
- Separate views for editing, deleting and adding species
- Checkboxes for bulk deleting of species from the counting list
- Checkboxes for bulk adding further species to the counting list
- Meta data enhanced with field for inspection note (replaces section notes)
- Preferences option added for sorting results either by section or species names
- CSV-export adapted to structural changes
- Internal butterfly list now identical with the list of TourCount
- Other code, text and layout improvements
- Licenses text separated
- Docs revised
- Bugs fixed

#### Version 4.0.3 (2024-03-10)
- Meta data page enhanced with start and end time values for temperature, wind and clouds
- Results page and csv-export file ditto
- One missing species with picture added
- Some missing local species names added
- Docs revised
- Bug fixed

#### Version 4.0.2 (2024-02-22)
- More pictures of species added
- Bugs fixed
- Docs updated
- Example DBs updated

#### Version 4.0.1 (2023-12-15)
##### Functional enhancement:
- Option for automatic transect section recognition per GPS. Therefore, a prepared track file in GPX format is needed
- GPS usage automatically determines between on- and off-track and shows appropriate counting screens
- Import function for a GPX file with GPS tracks of the transect sections

##### Changes:
- Import of DB and GPX files from Documents/TransektCount/ folder
- Export of DB, CSV and GPX files into Documents/TransektCount folder
- Restructured internal DB for the new functions
- Restructured table for the spreadsheet-compatible CSV-export
- More species added
- Missing pictures supplemented
- Docs updated with explanation how to create and handle an appropriate GPX file
- Code refinements
- Bugfixes
- Code adaptation for Android 14

#### Version 3.4.3 (2023-08-09)
- Last version without automatic transect sections recognition
- More missing species and pictures added
- Docs updated

#### Version 3.4.2 (2023-07-30)
- More missing species pictures added
- Edit section screen improved
- Some more modules translated from Java to Kotlin
- Docs updated

#### Version 3.4.0 (2023-07-05)
- Landscape mode removed (as hardly used, even on tablet)
- Garbage collection before creating sections list allows for more sections dependable on RAM amount
- To prevent DB corruption, deleting sections is reduced to the last listed section
- Some more modules translated from Java to Kotlin
- Some code refinements
- Docs updated

#### Version 3.3.8 (2023-06-20)
- Gradle upgraded -> 8.0
- Most deprecated functions replaced
- Permissions handling adapted to Android 11+
- .csv-files exported to Documents/TransektCount/ (allows access by other apps)
- Option to vibrate, short for counting-up and longer for counting-down
- Counting sound deeper when counting down
- One more species added
- Some more code improvements
- Sourcecode partly translated to Kotlin
- Minimal Android Version 7.1 (Nougat)

#### Version 3.3.7 (2023-03-26)
- Project adaption for Android Studio 2021.2.1 and Gradle 7.5
- Compiled with SDK Ver. 33 for target SDK Ver. 33
- Copyright and license texts updated
- Docs updated
- Some more species with pictures added
- Many pictures of species improved to show recto and verso sides
- Portrait/landscape handling improved
- Bug fixed in section lists editing

#### Version 3.3.0 (2022-04-17)
- Project adaption for Android Studio 2021.1.1 and Gradle 7.2
- Compiled for SDK version 31 (needed for current libs)
- Some more species with pictures added/improved
- Copyright and license texts updated
- Docs updated
- Some missing butterfly pictures added
- Outdated species names corrected (within app and example DBs)
- Clearer layout for Edit Transect Section
- Outdated code names updated
- Species family names updated where appropriate
- Fix for broken loading picture function since Gradle 6.5
- Bugs in "New section" and "Duplicate Section List" corrected

#### Version 3.2.5 (2020-09-09)
- Project adaption for Android Studio 4.0.1 and Gradle 6.1.1
- More butterfly pictures integrated
- Initial integrated list of species enlarged
- Background picture better scaled for long displays
- Preferences adapted for Android 10
- .CSV-Export: In Totals line suppression of zeros
- Strings cleared up
- Docs updated
- Bugfix in csv-export

#### Version 3.2.1 (2020-04-16)
- Code adapted for restricted Storage Access of Android 10
- Use of AndroidX libraries
- Project adaption for Android Studio 3.6.2
- Code optimized for storage and power efficiency
- App icon adapted to better match Android 9 screen
- Date and time of first count for a section
- Undercutting of species names in results page corrected
- Changes in AndroidManifest.xml and build.gradle
- Added/changed butterfly pictures
- Added total of different species counted to results page
- Added total of different species counted to exported csv-file
- Allow adding some species complexes to section lists
- Minimal changes to starting page
- Minor text corrections
- Help text updated
- Many minor code refinements
- Bug fixed for changing a section list name
- Bugfix: Crashed when returning from adding species without adding one
- Bugfixes for adding new section

#### Version 3.1.0 (2019-04-16)
- Integrated list of many European species
- Instead of manual input, add further species to your section lists by scroll-down selection from a comprehensive list that contains only species not contained yet
- Butterfly icons added to section editor
- Database structure supplemented for local butterfly names
- Example databases adapted fot local butterfly names
- Exported results include local butterfly names
- Additional butterfly pictures
- Project adaption for Android Studio 3.3.2 and Gradle 4.10.1
- Minimum Android version changed to 4.4 KitKat
- Additional butterfly pictures
- Some pictures edited to show both sides of wings
- Cosmetic changes to several app pages
- Docs updated
- Where applicable toasts replaced with snackbars
- Permission handling for Settings
- Additional manual input for date, start-time and stop-time with long press
- Code cleaning
- Unnecessary permission WRITE_SETTINGS removed
- Disabled multi-window-mode (Android 7+) for incompatible pull-down-menu (Spinner)
- Fix for crash when species selector is rapidly repeatedly pressed
- Fix for crash in permission handling of Android >= Marshmallow
- Fix for crash in "Edit Species List" functionality
- Bugfixes

#### Version 3.0.1 (2018-04-01)
- Project changes for Android Studio 3.1, Gradle 3.1.0 and SDK 27 (Android 8.1)
- First use now creates a direct usable internal DB with common european species
- Small optimizations on the results page
- Database version 3 (change of a column name for compiler warning) 
- Design of starting page updated
- Edit section now integrated in section list
- Added a camera button in counting menu
- Documentation adapted
- Docs: German Introductory presentation added
- Bugfixes

#### Version 2.1.0 (2017-09-27)
- Code adapted and compiled for Android 7.1
- Write system log only in debug version
- Added links in App Info and Help pages
- Added database version control to be compatible with old database structure prior version 2.0.0
- Added option to switch between portrait and landscape mode
- Results page shows totals first
- Results page sorted by 1. species and 2. sections
- CSV export table layout modified
- Show correct headline immediately after loading/resetting DB
- Fix for strange Spinner (Drop-Down-Selection) malfunction
- Stub picture if no picture of species available (e.g. group)
- Button sound also for selection of transect section
- Press back button twice to exit
- CSV export now suppresses zeros for empty counts
- CSV export adds overall totals
- Additional butterfly pictures
- Minor additions in Readme.md and other docs
- Minor layout improvements
- Minor code improvements
- Added Calendar Week in csv-export
- Fix crash when loading database of old structure prior version 2.0.0
- Bugfixes

#### Version 2.0.0 (2017-01-06)
- Completely revised counting page with species drop down selection and more complete count options
- Revised results page with detailed counting info und totals
- Revised species editor with bulk counting entries for all count options
- Check for missing or duplicate species codes
- New database structure according to the new count options
- Alerts for all counted butterfly imagos within counting boundary summed up
- Added pictures of species on counting and results page
- Create new section includes edit section
- Icons on settings page
- Icons in menus
- Codes for species added
- Option added: Sorting of species by names or codes
- "New Section" page simplified
- Results screen layout improved
- Option counting screen for lefties
- Exportable csv-table improved
- Switch screen off on counting page when in pocket
- Code improvement
- Bugfix

#### Version 1.3.0 (2016-06-22)
- Option "Screen bright" in Preferences
- Permission handling for Android M
- File import dialog for existing DB
- Message "Please wait..." when calling the counting or results page
- Show Transect-No. in app bar of the starting page
- Reject double species names in "Edit Sections"
- Reject double section names in "Duplicate Section"
- (+)-Button in app bar of page "List of Sections"
- New: "Reset Data" function
- New: Input page for master data
- New: Results page including master data
- New: Export functions including master data
- Background for editing pages unicolored
- App runs always in portrait mode
- Empty Basic DB may be exported any time
- New: Export DB in csv-Format
- Exported DB shows only counts >0
- Better views for small displays

#### Version 1.2.0 (2016-03-19)
- New: View for counting results
- Edit Section List: Initial focus on remarks field
- Alerts only for "internal" counts
- "Add Alert" now scrolls to end of page and sets focus on new field
- Design of Starting page improved
- Design of Counting page improved

#### Version 1.1.0 (2016-03-05)
- Layout of counting page improved
- Filename of Export-DB with Date
- (+)-Button in app bar of page "Add Species"
- "Add species" now scrolls to end of page and sets focus on new field
- Save Icon in app bar of page "New Section"
- Additional counter per species for external sightings. Therefore layout adapted in widgets and DB structure
- Also Database and calculating routines added for 2nd counter
- Count options allow to set values for both counters per species
- Section list shows remarks
- Section date shown only if not 0
- German text improved
- New: Detailed Help page
- Base-DB-Import: Raw Database 'transektcount0.db'
- Base-DB-Export: Raw Database 'transektcount0.db'
- DB-Export: Database 'transektcount.db'
- Sort default: Section name ascending
- German translation and adaptation.
- Removal of unused functions

#### Version 1.0.0 (2016-02-12)
##### 1. Release.
Counting and DB functionality are partly derived from BeeCount by Milo Thurston(knirirr)
(https://github.com/knirirr/BeeCount.git)

Main features comprise:
- Text and structure adaptation for butterfly monitoring in transects
- Modified AutoFitText.java, original code from author: pheuschk (open source code from stackoverflow)
- Modified ChangeLog.java original code from author: Karsten Priegnitz (open source code from code.google.com)
- Modified CSVWriter.java original code copyright 2015 Bytecode Pty Ltd. (Apache License 2.0)
- Modified File Chooser dialog based on android-file-chooser, 2011, Google Code Archive, GNU GPL v3
