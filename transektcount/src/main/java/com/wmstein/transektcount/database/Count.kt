package com.wmstein.transektcount.database

/***************************************************************************************************
 * Copyright © 2016-2026, Wilhelm Stein, Bonn, Germany
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
 * Table Count interface with records of transect count data
 *
 * Basic structure derived from Count.java of BeeCount created by Milo Thurston (knirirr)
 * on 2014-05-05, licensed under the Apache License, Version 2.0.
 *
 * Created by wmstein on 2016-02-18.
 * last edited in Java on 2019-03-22
 * converted to Kotlin on 2023-06-26
 * last edited on 2026-10-21
 **************************************************************************************************/

class Count {
    @JvmField
    var id = 0
    @JvmField
    var section_id = 0
    @JvmField
    var name = ""
    @JvmField
    var code = ""
    @JvmField
    var count_f1i = 0
    @JvmField
    var count_f2i = 0
    @JvmField
    var count_f3i = 0
    @JvmField
    var count_pi = 0
    @JvmField
    var count_li = 0
    @JvmField
    var count_ei = 0
    @JvmField
    var count_f1e = 0
    @JvmField
    var count_f2e = 0
    @JvmField
    var count_f3e = 0
    @JvmField
    var count_pe = 0
    @JvmField
    var count_le = 0
    @JvmField
    var count_ee = 0
    @JvmField
    var notes = ""
    @JvmField
    var name_g = ""

    fun increaseF1i(): Int {
        count_f1i += 1
        return count_f1i
    }

    fun increaseF2i(): Int {
        count_f2i += 1
        return count_f2i
    }

    fun increaseF3i(): Int {
        count_f3i += 1
        return count_f3i
    }

    fun increasePi(): Int {
        count_pi += 1
        return count_pi
    }

    fun increaseLi(): Int {
        count_li += 1
        return count_li
    }

    fun increaseEi(): Int {
        count_ei += 1
        return count_ei
    }

    fun increaseF1e(): Int {
        count_f1e += 1
        return count_f1e
    }

    fun increaseF2e(): Int {
        count_f2e += 1
        return count_f2e
    }

    fun increaseF3e(): Int {
        count_f3e += 1
        return count_f3e
    }

    fun increasePe(): Int {
        count_pe += 1
        return count_pe
    }

    fun increaseLe(): Int {
        count_le += 1
        return count_le
    }

    fun increaseEe(): Int {
        count_ee += 1
        return count_ee
    }

    // decreases
    fun safeDecreaseF1i(): Int {
        if (count_f1i > 0) {
            count_f1i -= 1
        }
        return count_f1i
    }

    fun safeDecreaseF2i(): Int {
        if (count_f2i > 0) {
            count_f2i -= 1
        }
        return count_f2i
    }

    fun safeDecreaseF3i(): Int {
        if (count_f3i > 0) {
            count_f3i -= 1
        }
        return count_f3i
    }

    fun safeDecreasePi(): Int {
        if (count_pi > 0) {
            count_pi -= 1
        }
        return count_pi
    }

    fun safeDecreaseLi(): Int {
        if (count_li > 0) {
            count_li -= 1
        }
        return count_li
    }

    fun safeDecreaseEi(): Int {
        if (count_ei > 0) {
            count_ei -= 1
        }
        return count_ei
    }

    fun safeDecreaseF1e(): Int {
        if (count_f1e > 0) {
            count_f1e -= 1
        }
        return count_f1e
    }

    fun safeDecreaseF2e(): Int {
        if (count_f2e > 0) {
            count_f2e -= 1
        }
        return count_f2e
    }

    fun safeDecreaseF3e(): Int {
        if (count_f3e > 0) {
            count_f3e -= 1
        }
        return count_f3e
    }

    fun safeDecreasePe(): Int {
        if (count_pe > 0) {
            count_pe -= 1
        }
        return count_pe
    }

    fun safeDecreaseLe(): Int {
        if (count_le > 0) {
            count_le -= 1
        }
        return count_le
    }

    fun safeDecreaseEe(): Int {
        if (count_ee > 0) {
            count_ee -= 1
        }
        return count_ee
    }
}