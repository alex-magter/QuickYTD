package org.alexmagter.QuickYTD

object TestNativeCode {
    external fun stringFromFFmpeg(): String

    init {
        System.loadLibrary("native-lib")
    }
}