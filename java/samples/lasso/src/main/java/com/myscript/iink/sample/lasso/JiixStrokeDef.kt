/*
 * Copyright (c) MyScript. All rights reserved.
 */

package com.myscript.iink.sample.lasso

import com.myscript.iink.graphics.Point
import java.io.Serializable

// Copyright MyScript. All rights reserved.


/**
 * Class definition used for Gson parsing
 */
object JiixStrokeDef {
	data class Stroke (
		var timestamp: String? = null,
		var X: FloatArray,
		var Y: FloatArray,
		var F: FloatArray,
		var T: IntArray,
	): Serializable {
		fun offset(offset: Point) {
			X.forEachIndexed { i, _ -> X[i] += offset.x  }
			Y.forEachIndexed { i, _ -> Y[i] += offset.y  }
		}
	}

	data class StrokeArray(
		var items: Array<Stroke>
	): Serializable
}
