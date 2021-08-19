/*
 * Copyright (c) MyScript. All rights reserved.
 */

package com.myscript.iink.app.common.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.text.method.ScrollingMovementMethod
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.PrintWriter
import java.io.StringWriter

import com.myscript.iink.app.common.R

open class ErrorActivity : AppCompatActivity() {
	private val tvErrorTitle by lazy { findViewById<TextView>(R.id.tv_error_title) }
	private val tvErrorMessage by lazy { findViewById<TextView>(R.id.tv_error_message) }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_error)
		tvErrorTitle.text = intent.getStringExtra(ERR_TITLE)
		tvErrorMessage.text = intent.getStringExtra(ERR_MESSAGE)
		tvErrorMessage.movementMethod = ScrollingMovementMethod()
	}


	private class ExceptionHandler constructor(
		private val ctx: Context
	) : Thread.UncaughtExceptionHandler {
		override fun uncaughtException(t: Thread, e: Throwable) {
			// get message from the root cause.
			var root: Throwable? = e
			while (root!!.cause != null) root = root.cause
			val message = root.message

			// print stack trace.
			val writer = StringWriter()
			e.printStackTrace(PrintWriter(writer))
			val trace = writer.toString()

			// launch the error activity.
			ctx.startActivity(Intent(ctx, ErrorActivity::class.java).apply {
				putExtra(ERR_TITLE, message)
				putExtra(ERR_MESSAGE, trace)
			})

			// kill the current activity.
			Process.killProcess(Process.myPid())
			System.exit(10)
		}
	}

	companion object {
		private val TAG = ErrorActivity::class.java.toString()
		private val ERR_TITLE = "err_title@$TAG"
		private val ERR_MESSAGE = "err_message@$TAG"
		fun setExceptionHandler(context: Context) {
			Thread.setDefaultUncaughtExceptionHandler(ExceptionHandler(context))
		}
	}
}