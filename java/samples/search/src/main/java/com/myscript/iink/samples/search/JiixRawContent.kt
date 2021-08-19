/*
 * Copyright (c) MyScript. All rights reserved.
 */

package com.myscript.iink.samples.search

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class JiixRawContent(
	@SerializedName("type")		var type: String? = null,
	@SerializedName("elements") var elements: List<Element>? = null,
): Serializable {
	data class Element(
		@SerializedName("type") var type: String? = null,
		@SerializedName("words") var words: List<Word>? = null,
		@SerializedName("chars") var chars: List<Char>? = null,
	) : Serializable

	data class Word(
		@SerializedName("label") var label: String? = null,
		@SerializedName("first-char") var firstChar: Int = 0,
		@SerializedName("last-char") var lastChar: Int = 0,
		@SerializedName("bounding-box") var boundingBox: BoundingBox? = null,
	) : Serializable

	data class Char(
		@SerializedName("label") var label: String? = null,
		@SerializedName("word") var word: Int = 0,
		@SerializedName("bounding-box") var boundingBox: BoundingBox? = null,
	) : Serializable

	data class BoundingBox(
		@SerializedName("x")		var x: Float = 0f,
		@SerializedName("y")		var y: Float = 0f,
		@SerializedName("width")	var width: Float = 0f,
		@SerializedName("height")	var height: Float = 0f,
	) : Serializable
}
