/*
 * Copyright (c) MyScript. All rights reserved.
 */

package com.myscript.iink.samples.search

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

import com.myscript.iink.*
//import com.myscript.iink.app.common.activities.ErrorActivity
import com.myscript.iink.uireferenceimplementation.EditorView
import com.myscript.iink.uireferenceimplementation.FontUtils
import com.myscript.iink.uireferenceimplementation.InputController
import java.io.File
import java.io.IOException
import java.lang.IllegalArgumentException

class MainActivity : AppCompatActivity(), View.OnClickListener {
	private var engine: Engine? = null
	private var contentPackage: ContentPackage? = null
	private var contentPart: ContentPart? = null
	private val editorView: EditorView by lazy { 
		findViewById<EditorView>(R.id.editor_view).apply { 
			typefaces = FontUtils.loadFontsFromAssets(applicationContext.assets)
		}
	}
	private val searchView: SearchView by lazy { findViewById<SearchView>(R.id.search_view) }
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
//		ErrorActivity.setExceptionHandler(this)
		setContentView(R.layout.activity_main)
		engine = IInkApplication.engine?.also {
			it.configuration.apply {
				setStringArray("configuration-manager.search-path", arrayOf("zip://$packageCodePath!/assets/conf"))
				setString("content-package.temp-folder",  "${filesDir.path}${File.separator}tmp")
				setBoolean("export.jiix.text.chars", true) // for partial word searching
				editorView.setEngine(it)
			}
		}
		searchView.visibility = View.INVISIBLE
		// load fonts
		searchView.setEditor(editorView.editor!!)
		editorView.editor?.addListener(object : IEditorListener {
			override fun partChanging(editor: Editor, oldPart: ContentPart?, newPart: ContentPart) {
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

		setInputMode(InputController.INPUT_MODE_FORCE_PEN) // If using an active pen, put INPUT_MODE_AUTO here
		val packageName = "File1.iink"
		val file = File(filesDir, packageName)
		try {
			contentPackage = engine?.createPackage(file)?.also { cpkg ->
				contentPart = cpkg.createPart("Raw Content")?.also { cp ->
					if (cp.type == "Raw Content") {
						editorView.editor?.configuration?.setBoolean("raw-content.recognition.shape", false)
						editorView.editor?.configuration?.setBoolean("raw-content.recognition.text", true) // Choose type of content (possible values are: "Text Document", "Text", "Diagram", "Math", "Drawing", and "Raw Content")
					}
				}
			}
			// text recognition of 'Raw Content' part must be enabled for search feature
		} catch (e: IOException) {
			Log.e(TAG, "Failed to open package \"$packageName\"", e)
		} catch (e: IllegalArgumentException) {
			Log.e(TAG, "Failed to open package \"$packageName\"", e)
		}
		title = "Type: ${contentPart!!.type}"

		// wait for view size initialization before setting part
		editorView.post(Runnable {
			editorView.renderer?.setViewOffset(0f, 0f)
			editorView.renderer?.viewScale = 1f
			editorView.visibility = View.VISIBLE

			// now search feature is only available for 'Raw Content' part
			if (contentPart!!.type == "Raw Content") searchView.visibility = View.VISIBLE
			editorView.editor?.part = contentPart
		})

		findViewById<View>(R.id.button_do_search).setOnClickListener(this)
		findViewById<View>(R.id.button_input_mode_forcePen).setOnClickListener(this)
		findViewById<View>(R.id.button_input_mode_forceTouch).setOnClickListener(this)
		findViewById<View>(R.id.button_input_mode_auto).setOnClickListener(this)
		findViewById<View>(R.id.button_undo).setOnClickListener(this)
		findViewById<View>(R.id.button_redo).setOnClickListener(this)
		findViewById<View>(R.id.button_clear).setOnClickListener(this)
		invalidateIconButtons()
	}

	override fun onDestroy() {
		editorView.setOnTouchListener(null)
		editorView.close()
		if (contentPart != null) {
			contentPart!!.close()
			contentPart = null
		}
		if (contentPackage != null) {
			contentPackage!!.close()
			contentPackage = null
		}

		// IInkApplication has the ownership, do not close here
//		engine = null
		super.onDestroy()
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.activity_main, menu)
		menu.findItem(R.id.menu_convert).apply { isEnabled = true }
		return super.onCreateOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.menu_convert -> {
				val editor = editorView.editor
				editor?.getSupportedTargetConversionStates(null)?.also {
					if (it.isNotEmpty()) editor.convert(null, it[0])
				}
				true
			}
			else -> {
				super.onOptionsItemSelected(item)
			}
		}
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_do_search -> doSearch()
			R.id.button_input_mode_forcePen -> setInputMode(InputController.INPUT_MODE_FORCE_PEN)
			R.id.button_input_mode_forceTouch -> setInputMode(InputController.INPUT_MODE_FORCE_TOUCH)
			R.id.button_input_mode_auto -> setInputMode(InputController.INPUT_MODE_AUTO)
			R.id.button_undo -> {
				editorView.editor?.undo()
				doSearch()
			}
			R.id.button_redo -> {
				editorView.editor?.redo()
				doSearch()
			}
			R.id.button_clear -> {
				editorView.editor?.clear()
				clearSearchResult()
			}
			else -> Log.e(TAG, "Failed to handle click event")
		}
	}

	private fun setInputMode(inputMode: Int) {
		editorView.inputMode = inputMode
		findViewById<View>(R.id.button_input_mode_forcePen).isEnabled =
			inputMode != InputController.INPUT_MODE_FORCE_PEN
		findViewById<View>(R.id.button_input_mode_forceTouch).isEnabled =
			inputMode != InputController.INPUT_MODE_FORCE_TOUCH
		findViewById<View>(R.id.button_input_mode_auto).isEnabled =
			inputMode != InputController.INPUT_MODE_AUTO
	}

	private fun invalidateIconButtons() {
		val editor = editorView.editor
		val canUndo = editor?.canUndo()
		val canRedo = editor?.canRedo()
		runOnUiThread {
			findViewById<ImageButton>(R.id.button_undo).apply { isEnabled = canUndo ?: false }
			findViewById<ImageButton>(R.id.button_redo).apply { isEnabled = canRedo ?: false } 
			findViewById<ImageButton>(R.id.button_clear).apply { isEnabled = contentPart != null } 
		}
	}

	private fun clearSearchResult() {
		(findViewById<View>(R.id.edit_search_text) as EditText).text.clear()
		searchView.clearSearchResult()
	}

	private fun doSearch() {
		val searchWord = (findViewById<View>(R.id.edit_search_text) as EditText).text.toString()
		searchView.doSearch(searchWord)
	}

	companion object {
		private const val TAG = "MainActivity"
	}
}