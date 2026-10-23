package com.wmstein.transektcount.database

/***************************************************************************************************
 * Copyright © 2023-2026, Wilhelm Stein, Bonn, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use any of its files except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************************************
 *
 * Table Track interface for records of the GPS track data for transect sections
 *
 * Created by wmstein on 2023-09-09,
 * last edited on 2026-10-21.
 **************************************************************************************************/

class Track {
    @JvmField
    var id = 0 // track point ID
    @JvmField
    var tsection = "" // track section name
    @JvmField
    var tlat = "" // track point latitude
    @JvmField
    var tlon = "" // track point longitude
}
