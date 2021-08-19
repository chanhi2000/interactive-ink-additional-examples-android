/*
 * Copyright (c) MyScript. All rights reserved.
 */

package com.myscript.iink.samples.batchmode

import android.app.Application
import com.myscript.certificate.MyCertificate
import com.myscript.iink.Engine

class IInkApplication : Application() {
	companion object {
		@get:Synchronized
		var engine: Engine? = null
			get() {
				if (field == null) { field = Engine.create(MyCertificate.getBytes()) }
				return field
			}
			private set
	}

}
