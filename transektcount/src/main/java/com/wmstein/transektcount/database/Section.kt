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
 * Table Section interface for records of transect sections data
 *
 * Based on Project.java of BeeCount crreated by Milo Thurston (knirirr) on 2014-05-05,
 * licensed under the Apache License, Version 2.0.
 *
 * Adopted for TransektCount by wmstein on 18.02.2016
 * last edited in Java on 2019-08-22,
 * converted to Kotlin on 2023-06-26,
 * last edited on 2026-10-21.
 **************************************************************************************************/

import android.annotation.SuppressLint

import java.text.SimpleDateFormat
import java.util.Date

class Section {
    @JvmField
    var id = 0

    @JvmField
    var created_at: Long = 0

    @JvmField
    var name = ""

    @JvmField
    var notes = ""

    //Get date and time from DB table sections field created_at
    val dateTime: String
        @SuppressLint("SimpleDateFormat")
        get() {
            val date = Date(created_at)
            val df = SimpleDateFormat("yyyy-MM-dd, HH:mm:ss")
            return df.format(date)
        }

    fun datNum(): Long {
        return created_at
    }

}