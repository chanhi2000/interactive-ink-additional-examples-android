/*
 * Copyright (c) MyScript. All rights reserved.
 */

package com.myscript.iink.sample.assessment

import android.app.Application
import com.myscript.certificate.MyCertificate
import com.myscript.iink.Engine
import java.io.File

class MyApplication : Application() {
	companion object {
		private var INSTANCE: MyApplication? = null
		fun getInstance() = INSTANCE
	}
	@get:Synchronized
	var engine: Engine? = null
		get() {
			if (field == null) {
				field = Engine.create(MyCertificate.getBytes()).also {
					it.configuration.apply {
						setStringArray("configuration-manager.search-path", arrayOf("zip://$packageCodePath!/assets/conf"))
						setString("content-package.temp-folder",filesDir.path + File.separator + "tmp")		// configure a temporary directory.
					}
				}
			}
			return field
		}
		private set

	override fun onCreate() {
		super.onCreate()
		INSTANCE = this
	}

	override fun onTerminate() {
		engine?.close()
		super.onTerminate()
	}
}
