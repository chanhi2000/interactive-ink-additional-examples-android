/*
 * Copyright (c) MyScript. All rights reserved.
 */

package com.myscript.iink.sample.lasso

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.myscript.iink.*
import com.myscript.iink.app.common.activities.ErrorActivity
//import com.myscript.iink.app.common.activities.ErrorActivity.Companion.setExceptionHandler
import com.myscript.iink.graphics.Transform
import com.myscript.iink.sample.lasso.JiixStrokeDef.StrokeArray
import com.myscript.iink.uireferenceimplementation.EditorView
import com.myscript.iink.uireferenceimplementation.FontMetricsProvider
import com.myscript.iink.uireferenceimplementation.ImageLoader
import com.myscript.iink.uireferenceimplementation.InputController
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity(), View.OnClickListener {
	private var engine: Engine? = null
	private val isBusy = AtomicBoolean(false)
	private val rootFrame: FrameLayout by lazy { findViewById<FrameLayout>(R.id.root_frame) }
	private val messageView: TextView by lazy { findViewById<TextView>(R.id.message_field) }
	val drawingView: EditorView by lazy { 
		(rootFrame.getChildAt(0) as EditorView).apply {  }
	}
	val lassoView: EditorView by lazy {
		(rootFrame.getChildAt(1) as EditorView).apply { 
			setBackgroundColor(Color.argb(0, 0, 0, 0))
			inputMode = InputController.INPUT_MODE_FORCE_PEN

		}
	}
	private lateinit var drawingEditor: Editor
	private lateinit var lassoEditor: Editor
	private var batchEditor: Editor? = null
	private val textPackageName = "package.iink"
	private val lassoPackageName = "lasso.iink"
	private val batchPackageName = "batch.iink"
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		ErrorActivity.setExceptionHandler(this)
		engine = IInkApplication.engine?.also { 
			it.configuration.apply { 
				setStringArray("configuration-manager.search-path", arrayOf("zip://$packageCodePath/assets/conf"))
				setString("content-package.temp-folder", "${filesDir.path}${File.separator}tmp")
				setBoolean("text.guides.enable", false)	
			}
			drawingView.setEngine(it)
			lassoView.setEngine(it)

			val displayMetrics = resources.displayMetrics
			// Create the batch editor
			batchEditor = it.createEditor(it.createRenderer(displayMetrics.xdpi, displayMetrics.ydpi, null))?.also { edt ->
				// The editor requires a font metrics provider and a view size *before* calling setPart()
				edt.setFontMetricsProvider(FontMetricsProvider(displayMetrics, HashMap<String, Typeface>()))
				edt.setViewSize(displayMetrics.widthPixels, displayMetrics.heightPixels)
			}
		}

		drawingEditor = drawingView.editor!!
		lassoEditor = lassoView.editor!!
		drawingView.imageLoader = ImageLoader(drawingEditor)
		lassoView.imageLoader = ImageLoader(lassoEditor)

		drawingEditor.addListener(object : IEditorListener {
			override fun partChanging(editor: Editor, oldPart: ContentPart?, newPart: ContentPart) {
				// no-op
			}

			override fun partChanged(editor: Editor) {
				invalidateOptionsMenu()
				invalidateIconButtons()
			}

			override fun contentChanged(editor: Editor, blockIds: Array<String>) {
				invalidateOptionsMenu()
				invalidateIconButtons()
			}

			override fun onError(editor: Editor, blockId: String, message: String) {
				Log.e(TAG, "Failed to edit block \"$blockId\"$message")
			}
		})
		lassoEditor.theme = "stroke { color: #00FFFFFF; }"
		lassoEditor.addListener(object : IEditorListener {
			override fun partChanging(editor: Editor, oldPart: ContentPart?, newPart: ContentPart) {
				// no-op
			}

			override fun partChanged(editor: Editor) {
				// no-op
			}

			override fun contentChanged(editor: Editor, blockIds: Array<String>) {
				onLasso()
			}

			override fun onError(editor: Editor, blockId: String, message: String) {
				Log.e(TAG, "Failed to edit block \"$blockId\"$message")
			}
		})
		var inputMode = InputController.INPUT_MODE_FORCE_PEN // If using an active pen, put INPUT_MODE_AUTO here
		if (savedInstanceState != null) inputMode = savedInstanceState.getInt(INPUT_MODE_KEY, inputMode)
		setInputMode(inputMode)

		// Create a renderer with a null render target
		// wait for view size initialization before setting part
		drawingView.post {
			configureEditor(drawingEditor, textPackageName, "Drawing")
			configureEditor(lassoEditor, lassoPackageName, "Drawing")
			configureEditor(batchEditor, batchPackageName, "Text")
			drawingView.visibility = View.VISIBLE
		}
		findViewById<View>(R.id.button_input_mode_forcePen).setOnClickListener(this)
		findViewById<View>(R.id.button_input_mode_forceTouch).setOnClickListener(this)
		findViewById<View>(R.id.button_input_mode_auto).setOnClickListener(this)
		findViewById<View>(R.id.button_lasso).setOnClickListener(this)
		findViewById<View>(R.id.button_undo).setOnClickListener(this)
		findViewById<View>(R.id.button_redo).setOnClickListener(this)
		findViewById<View>(R.id.button_clear).setOnClickListener(this)
		invalidateIconButtons()
	}


	private fun configureEditor(editor: Editor?, packageName: String, partType: String) {
		// Configure package a new package
		try {
			val contentPackage = engine!!.createPackage(packageName)
			val contentPart = contentPackage.createPart(partType)			// Create a new part
			editor!!.part = contentPart			// Associate editor with the new part
			contentPart.close()
			contentPackage.close()
		} catch (e: Exception) {
			showMessage("Failed to open package $packageName")
			Log.e(TAG, "Failed to open package $packageName", e)
		}
	}

	override fun onDestroy() {
		drawingView.setOnTouchListener(null)
		drawingView.close()
		lassoView.setOnTouchListener(null)
		lassoView.close()
		batchEditor!!.part = null
		try {
			engine?.deletePackage(textPackageName)
			engine?.deletePackage(lassoPackageName)
			engine?.deletePackage(batchPackageName)
		} catch (e: IOException) {
			showMessage("Failed to remove package ")
			Log.e(TAG, "Failed to remove package ", e)
		}

		// IInkApplication has the ownership, do not close here
		engine = null
		super.onDestroy()
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_input_mode_forcePen -> setInputMode(InputController.INPUT_MODE_FORCE_PEN)
			R.id.button_input_mode_forceTouch -> setInputMode(InputController.INPUT_MODE_FORCE_TOUCH)
			R.id.button_input_mode_auto -> setInputMode(InputController.INPUT_MODE_AUTO)
			R.id.button_lasso -> {
				findViewById<View>(R.id.button_lasso).isEnabled = false
				lassoView.visibility = View.VISIBLE
			}
			R.id.button_undo -> drawingEditor.undo()
			R.id.button_redo -> drawingEditor.redo()
			R.id.button_clear -> drawingEditor.clear()
			else -> Log.e(TAG, "Failed to handle click event")
		}
	}

	private fun setInputMode(inputMode: Int) {
		drawingView.inputMode = inputMode
		findViewById<View>(R.id.button_input_mode_forcePen).isEnabled =
			inputMode != InputController.INPUT_MODE_FORCE_PEN
		findViewById<View>(R.id.button_input_mode_forceTouch).isEnabled =
			inputMode != InputController.INPUT_MODE_FORCE_TOUCH
		findViewById<View>(R.id.button_input_mode_auto).isEnabled =
			inputMode != InputController.INPUT_MODE_AUTO
	}

	private fun invalidateIconButtons() {
		val canUndo = drawingEditor.canUndo()
		val canRedo = drawingEditor.canRedo()
		runOnUiThread {
			val imageButtonUndo = findViewById<ImageButton>(R.id.button_undo)
			imageButtonUndo.isEnabled = canUndo
			val imageButtonRedo = findViewById<ImageButton>(R.id.button_redo)
			imageButtonRedo.isEnabled = canRedo
			val imageButtonClear = findViewById<ImageButton>(R.id.button_clear)
			imageButtonClear.isEnabled = true
		}
	}

	private fun showMessage(msg: String) {
		runOnUiThread { messageView.text = msg }
	}

	private fun onLasso() {
		if (isBusy.getAndSet(true) || lassoEditor.isEmpty(null)) return
		Thread(Runnable {
			try {
				getDrawingStrokes(lassoEditor)?.also { lassoStroke ->
					if (lassoStroke.items.isNullOrEmpty()) {
						showMessage("No lasso stroke")
						return@Runnable
					}

					showMessage("Started batch on lasso analyse, Please wait...")
					// get lasso stroke
					val viewTransform = drawingEditor.renderer.viewTransform
					val offsetTransform = Transform(viewTransform).apply {
						invert()
					}
					val lasso = lassoStroke.items[0].apply {
						offset(offsetTransform.apply(0f, 0f))
					}
					val polyCorners: Int = lasso.X.size
					var j = polyCorners - 1
					val constant = FloatArray(polyCorners)
					val multiple = FloatArray(polyCorners)
					var i = 0
					while (i < polyCorners) {
						if (lasso.Y[j] == lasso.Y[i]) {
							constant[i] = lasso.X[i]
							multiple[i] = 0f
						} else {
							constant[i] =
								lasso.X[i] - lasso.Y[i] * lasso.X[j] / (lasso.Y[j] - lasso.Y[i]) + lasso.Y[i] * lasso.X[i] / (lasso.Y[j] - lasso.Y[i])
							multiple[i] = (lasso.X[j] - lasso.X[i]) / (lasso.Y[j] - lasso.Y[i])
						}
						j = i
						i++
					}
					val events: MutableList<PointerEvent> = ArrayList()
					val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS", Locale.US)
					getDrawingStrokes(drawingEditor)?.also { strokes ->
						for (stroke in strokes.items) {
							if (strokeInLasso(stroke, lasso, constant, multiple)) addToPtEvent(events, stroke, viewTransform, dateFormat)
						}
					}

					showMessage("Result: ${batchReco(events.toTypedArray())}")
					lassoEditor.clear()
					lassoEditor.waitForIdle()
					runOnUiThread {
						findViewById<View>(R.id.button_lasso).isEnabled = true
						lassoView.visibility = View.INVISIBLE
					}
				}

			} finally {
				isBusy.set(false)
			}
		}).start()
	}

	private fun getDrawingStrokes(editor: Editor?): StrokeArray? {
		val jiixString: String
		jiixString = try {
			editor!!.export_(null, MimeType.JIIX)
		} catch (e: Exception) {
			return null // when processing is ongoing, export may fail: ignore
		}
		var strokes: StrokeArray? = null
		try {
			val gson = Gson()
			strokes = gson.fromJson(jiixString, StrokeArray::class.java)
		} catch (e: JsonSyntaxException) {
			Log.e(
				TAG,
				"Failed to parse jiix string as json words: $e"
			)
		}
		return strokes
	}

	private fun strokeInLasso(
		stroke: JiixStrokeDef.Stroke,
		lasso: JiixStrokeDef.Stroke,
		constant: FloatArray,
		multiple: FloatArray
	): Boolean {
		val strokeLen: Int = stroke.X.size
		val lassoLen: Int = lasso.X.size
		for (pt in 0 until strokeLen) {
			var j = lassoLen - 1
			var oddNodes = false
			for (i in 0 until lassoLen) {
				if (lasso.Y[i] < stroke.Y[pt] && lasso.Y[j] >= stroke.Y[pt]
					|| lasso.Y[j] < stroke.Y[pt] && lasso.Y[i] >= stroke.Y[pt]
				) {
					oddNodes =
						oddNodes xor (stroke.Y[pt] * multiple[i] + constant[i] < stroke.X[pt])
				}
				j = i
			}
			if (oddNodes) return true
		}
		return false
	}

	private fun addToPtEvent(
		events: MutableList<PointerEvent>,
		stroke: JiixStrokeDef.Stroke,
		viewTransform: Transform,
		dateFormat: SimpleDateFormat
	) {
		val nbPt: Int = stroke.X.size
		var time: Long = 0
		for (i in 0 until nbPt) {
			val pt = viewTransform.apply(stroke.X[i], stroke.Y[i])
			if (i == 0) {
				try {
					val date = dateFormat.parse(stroke.timestamp)
					time = date.time
				} catch (e: ParseException) {
					Log.e(TAG, e.toString())
				}
				events.add(
					PointerEvent(
						PointerEventType.DOWN, pt.x, pt.y, time + stroke.T[i],
						stroke.F[i], PointerType.PEN, 0
					)
				)
			}
			if (i == nbPt - 1) events.add(
				PointerEvent(
					PointerEventType.UP, pt.x, pt.y, time + stroke.T[i],
					stroke.F[i], PointerType.PEN, 0
				)
			)
			if (i != 0 && i != nbPt - 1) events.add(
				PointerEvent(
					PointerEventType.MOVE, pt.x, pt.y, time + stroke.T[i],
					stroke.F[i], PointerType.PEN, 0
				)
			)
		}
	}

	private fun batchReco(pointerEvents: Array<PointerEvent>): String {
		var recoResult = ""
		// Feed the editor
		batchEditor?.pointerEvents(pointerEvents, false)
		batchEditor?.waitForIdle()
		try {
			recoResult = batchEditor!!.export_(null, MimeType.TEXT)
		} catch (e: Exception) {
			Log.e(TAG, "Failed to export recognition", e)
		}
		batchEditor?.clear()
		return recoResult
	}

	companion object {
		private const val TAG = "MainActivity"
		private const val INPUT_MODE_KEY = "inputMode"
	}
}
