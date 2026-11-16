package com.bjtniwa.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Nút “Từ khác” trên thông báo. */
class NextWordReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) { WordNotifier.show(context) }
}
