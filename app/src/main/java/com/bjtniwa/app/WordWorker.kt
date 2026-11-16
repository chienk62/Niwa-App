package com.bjtniwa.app

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

/** Chạy định kỳ (tối thiểu 15 phút một lần) để đổi từ vựng trên thanh thông báo. */
class WordWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        if (WordNotifier.prefs(applicationContext).getBoolean("enabled", false)) WordNotifier.show(applicationContext)
        return Result.success()
    }
    companion object { const val NAME = "niwa_word_notify" }
}
