/*
 * Copyright (c) MyScript. All rights reserved.
 */

package com.myscript.iink.samples.batchmode

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.ArrayList

data class JsonResult(
	@SerializedName("events") val strokes: ArrayList<Stroke>
): Serializable {
	override fun toString(): String = "{\"events\":[${strokes.joinToString(",")}]}"
}
