package com.wmstein.transektcount.database;

/********************************************
 * Based on Count.java by milo on 05/05/2014.
 * adopted and modified by wmstein since 2016-02-18
 * last edited on 2019-03-22
 */
public class Count
{
    public int id;
    public int section_id;
    public String name;
    public String code;
    public int count_f1i;
    public int count_f2i;
    public int count_f3i;
    public int count_pi;
    public int count_li;
    public int count_ei;
    public int count_f1e;
    public int count_f2e;
    public int count_f3e;
    public int count_pe;
    public int count_le;
    public int count_ee;
    public String notes;
    public String name_g;

    public int increase_f1i()
    {
        count_f1i = count_f1i + 1;
        return count_f1i;
    }

    public int increase_f2i()
    {
        count_f2i = count_f2i + 1;
        return count_f2i;
    }

    public int increase_f3i()
    {
        count_f3i = count_f3i + 1;
        return count_f3i;
    }

    public int increase_pi()
    {
        count_pi = count_pi + 1;
        return count_pi;
    }

    public int increase_li()
    {
        count_li = count_li + 1;
        return count_li;
    }

    public int increase_ei()
    {
        count_ei = count_ei + 1;
        return count_ei;
    }

    public int increase_f1e()
    {
        count_f1e = count_f1e + 1;
        return count_f1e;
    }

    public int increase_f2e()
    {
        count_f2e = count_f2e + 1;
        return count_f2e;
    }

    public int increase_f3e()
    {
        count_f3e = count_f3e + 1;
        return count_f3e;
    }

    public int increase_pe()
    {
        count_pe = count_pe + 1;
        return count_pe;
    }

    public int increase_le()
    {
        count_le = count_le + 1;
        return count_le;
    }

    public int increase_ee()
    {
        count_ee = count_ee + 1;
        return count_ee;
    }

    // decreases
    public int safe_decrease_f1i()
    {
        if (count_f1i > 0)
        {
            count_f1i = count_f1i - 1;
        }
        return count_f1i;
    }

    public int safe_decrease_f2i()
    {
        if (count_f2i > 0)
        {
            count_f2i = count_f2i - 1;
        }
        return count_f2i;
    }

    public int safe_decrease_f3i()
    {
        if (count_f3i > 0)
        {
            count_f3i = count_f3i - 1;
        }
        return count_f3i;
    }

    public int safe_decrease_pi()
    {
        if (count_pi > 0)
        {
            count_pi = count_pi - 1;
        }
        return count_pi;
    }

    public int safe_decrease_li()
    {
        if (count_li > 0)
        {
            count_li = count_li - 1;
        }
        return count_li;
    }

    public int safe_decrease_ei()
    {
        if (count_ei > 0)
        {
            count_ei = count_ei - 1;
        }
        return count_ei;
    }

    public int safe_decrease_f1e()
    {
        if (count_f1e > 0)
        {
            count_f1e = count_f1e - 1;
        }
        return count_f1e;
    }

    public int safe_decrease_f2e()
    {
        if (count_f2e > 0)
        {
            count_f2e = count_f2e - 1;
        }
        return count_f2e;
    }

    public int safe_decrease_f3e()
    {
        if (count_f3e > 0)
        {
            count_f3e = count_f3e - 1;
        }
        return count_f3e;
    }

    public int safe_decrease_pe()
    {
        if (count_pe > 0)
        {
            count_pe = count_pe - 1;
        }
        return count_pe;
    }

    public int safe_decrease_le()
    {
        if (count_le > 0)
        {
            count_le = count_le - 1;
        }
        return count_le;
    }

    public int safe_decrease_ee()
    {
        if (count_ee > 0)
        {
            count_ee = count_ee - 1;
        }
        return count_ee;
    }

}
