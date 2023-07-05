package com.wmstein.transektcount.database

/********************************************
 * Based on Count.java by milo on 05/05/2014.
 * adopted and modified by wmstein since 2016-02-18
 * last edited in Java on 2019-03-22
 * converted to Kotlin on 2023-06-26
 */
class Count {
    @JvmField
    var id = 0
    @JvmField
    var section_id = 0
    @JvmField
    var name: String? = null
    @JvmField
    var code: String? = null
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
    var notes: String? = null
    @JvmField
    var name_g: String? = null
    fun increase_f1i(): Int {
        count_f1i = count_f1i + 1
        return count_f1i
    }

    fun increase_f2i(): Int {
        count_f2i = count_f2i + 1
        return count_f2i
    }

    fun increase_f3i(): Int {
        count_f3i = count_f3i + 1
        return count_f3i
    }

    fun increase_pi(): Int {
        count_pi = count_pi + 1
        return count_pi
    }

    fun increase_li(): Int {
        count_li = count_li + 1
        return count_li
    }

    fun increase_ei(): Int {
        count_ei = count_ei + 1
        return count_ei
    }

    fun increase_f1e(): Int {
        count_f1e = count_f1e + 1
        return count_f1e
    }

    fun increase_f2e(): Int {
        count_f2e = count_f2e + 1
        return count_f2e
    }

    fun increase_f3e(): Int {
        count_f3e = count_f3e + 1
        return count_f3e
    }

    fun increase_pe(): Int {
        count_pe = count_pe + 1
        return count_pe
    }

    fun increase_le(): Int {
        count_le = count_le + 1
        return count_le
    }

    fun increase_ee(): Int {
        count_ee = count_ee + 1
        return count_ee
    }

    // decreases
    fun safe_decrease_f1i(): Int {
        if (count_f1i > 0) {
            count_f1i = count_f1i - 1
        }
        return count_f1i
    }

    fun safe_decrease_f2i(): Int {
        if (count_f2i > 0) {
            count_f2i = count_f2i - 1
        }
        return count_f2i
    }

    fun safe_decrease_f3i(): Int {
        if (count_f3i > 0) {
            count_f3i = count_f3i - 1
        }
        return count_f3i
    }

    fun safe_decrease_pi(): Int {
        if (count_pi > 0) {
            count_pi = count_pi - 1
        }
        return count_pi
    }

    fun safe_decrease_li(): Int {
        if (count_li > 0) {
            count_li = count_li - 1
        }
        return count_li
    }

    fun safe_decrease_ei(): Int {
        if (count_ei > 0) {
            count_ei = count_ei - 1
        }
        return count_ei
    }

    fun safe_decrease_f1e(): Int {
        if (count_f1e > 0) {
            count_f1e = count_f1e - 1
        }
        return count_f1e
    }

    fun safe_decrease_f2e(): Int {
        if (count_f2e > 0) {
            count_f2e = count_f2e - 1
        }
        return count_f2e
    }

    fun safe_decrease_f3e(): Int {
        if (count_f3e > 0) {
            count_f3e = count_f3e - 1
        }
        return count_f3e
    }

    fun safe_decrease_pe(): Int {
        if (count_pe > 0) {
            count_pe = count_pe - 1
        }
        return count_pe
    }

    fun safe_decrease_le(): Int {
        if (count_le > 0) {
            count_le = count_le - 1
        }
        return count_le
    }

    fun safe_decrease_ee(): Int {
        if (count_ee > 0) {
            count_ee = count_ee - 1
        }
        return count_ee
    }
}