/*
 * Copyright (c) MyScript. All rights reserved.
 */

package com.myscript.iink.samples.search

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.google.gson.Gson
import com.myscript.iink.Editor
import com.myscript.iink.MimeType
import java.lang.Exception
import java.util.*

class SearchView @JvmOverloads constructor(
	ctx: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
) : View(ctx, attrs, defStyleAttr) {
	private var editor: Editor? = null
	private var bitmap: Bitmap? = null
	private var sysCanvas: Canvas? = null
	private var jiixString: String? = null
	private var searchWord: String? = null
	private var searchRects: ArrayList<Rect>? = null
	var paint = Paint()

	override fun onDraw(canvas: Canvas) {
		if (sysCanvas == null || bitmap == null) return
		paint.color = DEFAULT_COLOR
		paint.alpha = DEFAULT_ALPHA
		sysCanvas?.save()
		sysCanvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
		searchRects?.let {
			it.forEach { r -> sysCanvas?.drawRect(r, paint) }
		}
		sysCanvas?.restore()
		canvas.drawBitmap(bitmap!!, 0f, 0f, null)
	}

	override fun onSizeChanged(newWidth: Int, newHeight: Int, oldWidth: Int, oldHeight: Int) {
		bitmap?.recycle()
		bitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888).also {
			sysCanvas = Canvas(it)
		}
		update()
		super.onSizeChanged(newWidth, newHeight, oldWidth, oldHeight)
	}

	fun setEditor(editor: Editor) {
		this.editor = editor
		jiixString = ""
		searchWord = ""
		searchRects = ArrayList()
	}

	fun clearSearchResult() {
		searchWord = ""
		jiixString = ""
		update()
	}

	fun doSearch(searchWord: String?) {
		checkNotNull(editor) { "Must not be called before setEditor()" }
		this.searchWord = searchWord
		try {
			jiixString = editor!!.export_(editor!!.rootBlock, MimeType.JIIX)
		} catch (e: Exception) {
			e.printStackTrace()
		}
		update()
	}

	fun update() {
		if (jiixString == null || searchWord == null || searchRects == null) return
		findSearchResultRects()
		postInvalidate()
	}

	private fun findSearchResultRects() {
		checkNotNull(editor) { "Must not be called before setEditor()" }
		searchRects!!.clear()
		if (jiixString == "") return
		try {
			Gson().fromJson(
				jiixString,
				JiixRawContent::class.java
			)?.also {
				if (it.type == "Raw Content") {
					for (element in it.elements!!) {
						if (element.type == "Text") {
							for (word in element.words!!) {
								val conf = editor!!.engine.configuration
								if (conf.getBoolean("export.jiix.text.chars")) findRectForPartialWord(
									element,
									word
								) else findRectForFullWord(word)
							}
						}
					}
				}
			}
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	private fun findRectForFullWord(word: JiixRawContent.Word) {
		if (word.boundingBox != null && word.label.equals(searchWord!!, ignoreCase = true)) {
			val rect = getPixelRect(word.boundingBox!!)
			searchRects!!.add(rect)
		}
	}

	private fun findRectForPartialWord(element: JiixRawContent.Element, word: JiixRawContent.Word) {
		val offset: Int = word.label!!.toLowerCase(Locale.getDefault()).indexOf(searchWord!!.toLowerCase(Locale.getDefault()))
		if (offset >= 0) {
			val boundingRect = Rect()
			var length = 0
			val firstChar: Int = word.firstChar
			val lastChar: Int = word.lastChar
			for (i in firstChar + offset until lastChar + 1) {
				length += 1
				if (length > searchWord!!.length) break
				val charElement: JiixRawContent.Char = element.chars!![i]
				val rect = getPixelRect(charElement.boundingBox!!)
				boundingRect.union(rect)
			}
			searchRects!!.add(boundingRect)
		}
	}

	private fun getPixelRect(boundingBox: JiixRawContent.BoundingBox): Rect {
		checkNotNull(editor) { "Must not be called before setEditor()" }
		val transform = editor!!.renderer.viewTransform
		val rect = Rect()
		val x: Float = boundingBox.x
		val y: Float = boundingBox.y
		val width: Float = boundingBox.width
		val height: Float = boundingBox.height
		val pointStart = transform.apply(x, y)
		val pointEnd = transform.apply(x + width, y + height)
		rect.left = pointStart.x.toInt()
		rect.top = pointStart.y.toInt()
		rect.right = pointEnd.x.toInt()
		rect.bottom = pointEnd.y.toInt()
		return rect
	}

	companion object {
		private const val DEFAULT_ALPHA = 60
		private const val DEFAULT_COLOR = Color.RED
	}
}