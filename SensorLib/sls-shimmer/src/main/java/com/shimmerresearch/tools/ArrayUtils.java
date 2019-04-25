package com.shimmerresearch.tools;

public class ArrayUtils
{
    public static void reverse(byte[] a)
    {
        int n = a.length;
        int i, k;
        byte t;
        for (i = 0; i < n / 2; i++) {
            t = a[i];
            a[i] = a[n - i - 1];
            a[n - i - 1] = t;
        }
    }
}
