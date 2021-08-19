/*
 * Copyright (c) MyScript. All rights reserved.
 */

package com.myscript.iink.sample.assessment.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.myscript.iink.*
//import com.myscript.iink.app.common.activities.ErrorActivity
import com.myscript.iink.sample.assessment.R
import com.myscript.iink.sample.assessment.MyApplication
import com.myscript.iink.uireferenceimplementation.EditorView
import com.myscript.iink.uireferenceimplementation.FontUtils
import com.myscript.iink.uireferenceimplementation.InputController
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity(), IEditorListener {
	val problemSolver1: View by lazy { findViewById<View>(R.id.problemSolver1) }
	val problemSolver2: View by lazy { findViewById<View>(R.id.problemSolver2) }
	private val answerEditorView1: EditorView by lazy { problemSolver1.findViewById<View>(R.id.answerEditor).findViewById<EditorView>(R.id.editor_view) }
	private val answerEditorView2: EditorView by lazy { problemSolver2.findViewById<View>(R.id.answerEditor).findViewById<EditorView>(R.id.editor_view) }
	private val scoreEditorView1: EditorView by lazy { problemSolver1.findViewById<View>(R.id.scoreEditor).findViewById<EditorView>(R.id.editor_view) }
	private val scoreEditorView2: EditorView by lazy { problemSolver2.findViewById<View>(R.id.scoreEditor).findViewById<EditorView>(R.id.editor_view) }
	private var activeEditorView: EditorView? = null

	private var engine: Engine? = null
	private var contentPackage: ContentPackage? = null
	private var answerContentPart1: ContentPart? = null
	private var answerContentPart2: ContentPart? = null
	private var scoreContentPart1: ContentPart? = null
	private var scoreContentPart2: ContentPart? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
//		ErrorActivity.setExceptionHandler(this)
		engine = MyApplication.getInstance()?.engine?.also {
			try {
				contentPackage = it.createPackage(File(filesDir, IINK_PACKAGE_NAME))
			} catch (e: IOException) {
				e.printStackTrace()
			}
		}
		if (contentPackage == null) return
		initWith(contentPackage!!)
		initWith(answerEditorView1)
		initWith(answerEditorView2)
		initWith(scoreEditorView1)
		initWith(scoreEditorView2)
	}

	private fun initWith(editorView: EditorView) {
		editorView.typefaces = FontUtils.loadFontsFromAssets(applicationContext.assets)
		editorView.setEngine(engine!!)
		val editor = editorView.editor ?: return
		editor.addListener(this)
		// TODO: try different input modes:
		// - InputController.INPUT_MODE_AUTO
		// - InputController.INPUT_MODE_NONE
		// - InputController.INPUT_MODE_FORCE_PEN
		// - InputController.INPUT_MODE_FORCE_TOUCH
		editorView.inputMode = InputController.INPUT_MODE_FORCE_PEN
		// attach content part to the editor.
		editorView.post(Runnable {
			val contentPart: ContentPart?  =
				when {
					editorView === answerEditorView1 -> answerContentPart1
					editorView === answerEditorView2 -> answerContentPart2
					editorView === scoreEditorView1 -> scoreContentPart1
					editorView === scoreEditorView2 -> scoreContentPart2
					else -> return@Runnable
				}
			editor.configuration?.also {
				when (contentPart?.type) {
					"Math" -> it.setBoolean("math.solver.enable", false) // disable math solver result
					"Text" -> it.setBoolean("text.guides.enable", false) // disable text guide lines
					else -> {}
				}
			}
			editor.part = contentPart
			editorView.visibility = View.VISIBLE
		})
	}

	private fun initWith(contentPackage: ContentPackage) {
		// TODO: try different part types: Diagram, Drawing, Math, Text, Text Document.
		answerContentPart1 = contentPackage.createPart("Math")
		answerContentPart2 = contentPackage.createPart("Text")
		scoreContentPart1 = contentPackage.createPart("Text")
		scoreContentPart2 = contentPackage.createPart("Text")
	}

	override fun onDestroy() {
		answerContentPart1!!.close()
		answerEditorView1!!.close()
		scoreContentPart1!!.close()
		scoreEditorView1!!.close()
		answerContentPart2!!.close()
		answerEditorView2!!.close()
		scoreContentPart2!!.close()
		scoreEditorView2!!.close()
		contentPackage!!.close()
		super.onDestroy()
	}

	// region implementations (options menu)
	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.activity_main, menu)
		return super.onCreateOptionsMenu(menu)
	}

	override fun onPrepareOptionsMenu(menu: Menu): Boolean {
		if (activeEditorView == null) return super.onPrepareOptionsMenu(menu)
		val editor = activeEditorView!!.editor
		menu.findItem(R.id.menu_redo).isEnabled = editor != null && editor.canRedo()
		menu.findItem(R.id.menu_undo).isEnabled = editor != null && editor.canUndo()
		if (editor == null) return super.onPrepareOptionsMenu(menu)
		val contentPart = editor.part
		menu.findItem(R.id.menu_clear).isEnabled = contentPart != null && !contentPart.isClosed
		return super.onPrepareOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		if (activeEditorView == null) return super.onOptionsItemSelected(item)
		val editor = activeEditorView!!.editor
		if (editor != null) {
			if (!editor.isIdle) editor.waitForIdle()
			when (item.itemId) {
				R.id.menu_clear -> editor.clear()
				R.id.menu_convert -> {
					val conversionStates = editor.getSupportedTargetConversionStates(null)
					if (conversionStates.size != 0) editor.convert(null, conversionStates[0])
				}
				R.id.menu_redo -> editor.redo()
				R.id.menu_undo -> editor.undo()
				else -> {
				}
			}
		}
		return super.onOptionsItemSelected(item)
	}

	// endregion
	// region implementations (IEditorListener)
	override fun partChanging(editor: Editor, oldPart: ContentPart, newPart: ContentPart) {
		invalidateOptionsMenu()
	}

	override fun partChanged(editor: Editor) {
		invalidateOptionsMenu()
	}

	override fun contentChanged(editor: Editor, blockIds: Array<String>) {
		invalidateOptionsMenu()
		activeEditorView =
			if (editor === answerEditorView1.editor) answerEditorView1
			else if (editor === answerEditorView2.editor) answerEditorView2
			else if (editor === scoreEditorView1.editor) scoreEditorView1
			else if (editor === scoreEditorView2!!.editor) scoreEditorView2
			else null
	}

	override fun onError(editor: Editor, blockId: String, message: String) {
		if (activeEditorView == null) return
		activeEditorView?.post {
			Toast.makeText(activeEditorView!!.context, message, Toast.LENGTH_LONG).show()
		}
	} // endregion

	companion object {
		private const val IINK_PACKAGE_NAME = "my_iink_package"
	}
}