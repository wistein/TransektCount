# History

#### TransektCount Copyright (C) 2016-2018, Wilhelm Stein

#### Version 3.0.5 (2018-08-11)
- Additional butterfly pictures
- Some pictures edited to show both sides of wings
- Some docs updated

#### Version 3.0.4 (2018-08-04)
- Additional butterfly pictures
- Where applicable toasts replaced with snackbars
- Permission handling for Settings
- Some code cleaning
- Fix for crash in permission handling of Android >= Marshmallow
- Fix for crash in "Edit Species List" functionality

#### Version 3.0.3 (2018-05-01)
- Additional manual input for date, start-time and stop-time with long press
- Project changes for Android Studio 3.1.2 and Gradle 3.1.2
- Code cleaning
- Bug fixes

#### Version 3.0.2 (2018-04-05)
- Unnecessary permission WRITE_SETTINGS removed
- Disabled multi-window-mode (Android 7+) for incompatible pull-down-menu (Spinner)
- Code cleaning
- Bug fixes

#### Version 3.0.1 (2018-04-01)
- Corrected wrong GitHub Tag

#### Version 3.0.0 (2018-03-30)
- Project changes for Android Studio 3.1, Gradle 3.1.0 and SDK 27 (Android 8.1)
- First use now creates a direct usable internal DB with common european species
- Small optimizations on the results page
- Database version 3 (change of a column name for compiler warning) 
- Documentation adapted
- Bug fixes

#### Version 2.1.6 (2018-03-19)
- Edit section now also integrated in section list
- Added a camera button in counting menu
- Bug fixes

#### Version 2.1.1 (2017-10-31)
- Dev. environment adapted for Android Studio 3.0
- Docs: German Introductary presentation added
- Bug fixes

#### Version 2.1.0 (2017-09-27)
- Code adapted and compiled for Android 7.1
- Minor code improvements
- Bug fixes

#### Version 2.0.6 (2017-09-11)
- Write log only when MyDebug=true

#### Version 2.0.5 (2017-09-10)
- Added links in App Info and Help pages
- Added database version control to be compatible with old database structure prior version 2.0.0
- Fix crash when loading database of old structure prior version 2.0.0
- Bugfixes

#### Version 2.0.4 (2017-08-29)
- Added option to switch between portrait and landscape mode
- Results page shows totals first
- Results page sorted by 1. species and 2. sections
- Show correct headline immediately after loading/resetting DB
- CSV export table layout modified
- Minor bug fixes

#### Version 2.0.3 (2017-08-04)
- Fix for strange Drop-Down-Selection (Spinner) malfunction

#### Version 2.0.2 (2017-07-13)
- Stub picture if no picture of species available (e.g. group)
- Button sound also for selection of transect section
- Press back button twice to exit
- CSV export now suppresses zeros for empty counts
- CSV export adds overall totals

#### Version 2.0.1 (2017-04-14)
- Minor additions in Readme.md and other docs
- Minor layout improvements
- Added Calendar Week in csv-export
- Additional butterfly pictures

#### Version 2.0.0 (2017-01-06)
- Completely revised counting page with species drop down selection and more complete count options
- Revised results page with detailed counting info und totals
- Revised species editor with bulk counting entries for all count options
- Check for missing or duplicate species codes
- New database structure according to the new count options
- Alerts for all counted butterflies within counting boundary (♂, ♀ summed up)

#### Version 1.3.9 (2016-11-21)
- Pictures of species on counting page
- Create new section includes edit section

#### Version 1.3.8 (2016-10-27)
- Code improvement
- Icons on settings page
- Icons in menus
- Codes for species added
- Option added: Sorting of species by names or codes
- "New Section" page simplified

#### Version 1.3.5 (2016-10-03)
- Results screen layout improved
- Option counting screen for lefties
- Exportable csv-table improved
- Bug fix

#### Version 1.3.2 (2016-08-04)
- Bug fix
- Code optimized

#### Version 1.3.1 (2016-07-25)
- Switch screen off on counting page when in pocket

#### Version 1.3.0 (2016-06-22)
- Option "Screen bright" in Preferences

#### Version 1.2.7 (2016-06-11)
- Permission handling for Android M
- File import dialog for existing DB

#### Version 1.2.6 (2016-04-14)
- Message "Please wait..." when calling the counting or results page
- Show Transect-No. in app bar of the starting page

#### Version 1.2.5 (2016-04-07)
- Reject double species names in "Edit Sections"
- Reject double section names in "Duplicate Section"
- "+"-Button in app bar of page "List of Sections"

#### Version 1.2.4 (2016-04-04)
- New: "Reset Data" function
- New: Input page for master data
- New: Results page including master data
- New: Export functions including master data
- Background for editing pages unicolored

#### Version 1.2.3 (2016-03-28)
- App runs always in portrait mode
- Empty Basic DB may be exported any time

#### Version 1.2.2 (2016-03-25)
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
- "+"-Button in app bar of page "Add Species"
- "Add species" now scrolls to end of page and sets focus on new field
- Save Icon in app bar of page "New Section"

#### Version 1.0.8 (2016-02-27)
- Additional counter per species for external sightings. Therefore layout adapted in widgets and DB structure
- Also Database and calculating routines added for 2nd counter
- Count options allow to set values for both counters per species

#### Version 1.0.6 (2016-02-22)
- Section list shows remarks
- Section date shown only if not 0
- German text improved
- Some minor code improvements

#### Version 1.0.4 (2016-02-18)
- New: Detailed Help page
- Base-DB-Import: Raw Database 'transektcount0.db'
- Base-DB-Export: Raw Database 'transektcount0.db'
- DB-Export: Database 'transektcount.db'

#### Version 1.0.2 (2016-02-14)
- Sort default: Section name ascending
- German translation and adaptation.
- Removal of unused functions

#### Version 1.0.0 (2016-02-12)
##### 1. Release.
TransektCount originally derives partly from BeeCount by knirirr
(https://github.com/knirirr/BeeCount.git)

Main features comprise
- Text and structure adaptation for butterfly monitoring in transects
- Modified AutoFitText.java, original code from author: pheuschk (open source code from stackoverflow)
- Modified ChangeLog.java original code from author: Karsten Priegnitz (open source code from code.google.com)
- Modified CSVWriter.java original code copyright 2015 Bytecode Pty Ltd. (Apache License 2.0)
- Modified File Chooser dialog based on android-file-chooser, 2011, Google Code Archive, GNU GPL v3
