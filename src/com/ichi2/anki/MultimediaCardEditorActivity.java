package com.ichi2.anki;

import java.io.File;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ichi2.anki.multimediacard.AudioView;
import com.ichi2.anki.multimediacard.IField;
import com.ichi2.anki.multimediacard.IMultimediaEditableNote;
import com.ichi2.anki.multimediacard.impl.MockNoteFactory;

public class MultimediaCardEditorActivity extends Activity
{
	public static final String EXTRA_CALLER = "CALLER";
	public static final int CALLER_NOCALLER = 0;
	public static final int CALLER_INDICLASH = 10;

	public static final int REQUEST_CODE_EDIT_FIELD = 1;

	public static final String EXTRA_FIELD_INDEX = "multim.card.ed.extra.field.index";
	public static final String EXTRA_FIELD = "multim.card.ed.extra.field";
	public static final String EXTRA_WHOLE_NOTE = "multim.card.ed.extra.whole.note";

	private static final String ACTION_CREATE_FLASHCARD = "org.openintents.action.CREATE_FLASHCARD";
	private static final String ACTION_CREATE_FLASHCARD_SEND = "android.intent.action.SEND";

	IMultimediaEditableNote mNote;
	private LinearLayout mMainUIiLayout;

	/* indicates if a new fact is added or a card is edited */
	private boolean mAddNote;

	/* indicates which activity called card editor */
	private int mCaller;

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_multimedia_card_editor);

		// Handling absence of the action bar!
		int currentapiVersion = android.os.Build.VERSION.SDK_INT;
		if (currentapiVersion <= android.os.Build.VERSION_CODES.GINGERBREAD_MR1)
		{
			LinearLayout linearLayout = (LinearLayout) findViewById(R.id.LinearLayoutForSpareMenu);
			createSpareMenu(linearLayout);
		}

		mMainUIiLayout = (LinearLayout) findViewById(R.id.LinearLayoutInScrollView);

		if (savedInstanceState != null)
		{
			mCaller = savedInstanceState.getInt("caller");
			mAddNote = savedInstanceState.getBoolean("addFact");
		}
		else
		{
			_loadNote();
		}

		recreateUi(getNotePrivate());
	}

	private IMultimediaEditableNote getNotePrivate()
	{
		if (mNote == null)
		{
			mNote = _loadNote();
		}

		return mNote;
	}

	private void createSpareMenu(LinearLayout linearLayout)
	{
		Button saveButton = new Button(this);
		saveButton.setText(getString(R.string.CardEditorSaveButton));
		saveButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				save();
			}
		});
		linearLayout.addView(saveButton);

		Button deleteButton = new Button(this);
		deleteButton.setText(getString(R.string.menu_delete_note));
		deleteButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				delete();
			}
		});
		linearLayout.addView(deleteButton);
	}

	private void recreateUi(IMultimediaEditableNote note)
	{

		LinearLayout linearLayout = mMainUIiLayout;

		linearLayout.removeAllViews();

		for (int i = 0; i < note.getNumberOfFields(); ++i)
		{
			createNewViewer(linearLayout, note.getField(i), i);
		}

	}

	private void putExtrasAndStartEditActivity(final IField field, final int index, Intent i)
	{

		i.putExtra(EXTRA_FIELD_INDEX, index);
		i.putExtra(EXTRA_FIELD, field);
		i.putExtra(EXTRA_WHOLE_NOTE, mNote);

		startActivityForResult(i, REQUEST_CODE_EDIT_FIELD);
	}

	private void createNewViewer(LinearLayout linearLayout, final IField field, final int index)
	{

		final MultimediaCardEditorActivity context = this;

		switch (field.getType())
		{
			case TEXT:

				// Create a text field and an edit button, opening editing for
				// the
				// text field

				TextView textView = new TextView(this);
				textView.setMinLines(3);
				textView.setText(field.getText());
				linearLayout.addView(textView, LinearLayout.LayoutParams.MATCH_PARENT);

				break;

			case IMAGE:

				ImageView imgView = new ImageView(this);
				//
				// BitmapFactory.Options options = new BitmapFactory.Options();
				// options.inSampleSize = 2;
				// Bitmap bm = BitmapFactory.decodeFile(myJpgPath, options);
				// jpgView.setImageBitmap(bm);

				imgView.setImageURI(Uri.fromFile(new File(field.getImagePath())));
				linearLayout.addView(imgView, LinearLayout.LayoutParams.MATCH_PARENT);

				break;
			case AUDIO:
				AudioView audioView = new AudioView(this, R.drawable.av_play, R.drawable.av_pause, R.drawable.av_stop,
						R.drawable.av_rec, R.drawable.av_rec_stop, field.getAudioPath());
				audioView.setRecordButtonVisible(false);
				linearLayout.addView(audioView);
				break;

			default:
				TextView unsupp = new TextView(this);
				unsupp.setText("Unsupported field type");
				unsupp.setEnabled(false);
				linearLayout.addView(unsupp);
				break;
		}

		Button editButtonText = new Button(this);
		editButtonText.setText("Edit");
		linearLayout.addView(editButtonText, LinearLayout.LayoutParams.MATCH_PARENT);

		editButtonText.setOnClickListener(new View.OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				Intent i = new Intent(context, EditFieldActivity.class);
				putExtrasAndStartEditActivity(field, index, i);
			}

		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.activity_multimedia_card_editor, menu);
		return true;
	}

	// Loads from extras or whatever else
	private IMultimediaEditableNote _loadNote()
	{
		Intent intent = getIntent();
		mCaller = intent.getIntExtra(EXTRA_CALLER, CALLER_NOCALLER);
		if (mCaller == CALLER_NOCALLER)
		{
			String action = intent.getAction();
			if (action != null
					&& (ACTION_CREATE_FLASHCARD.equals(action) || ACTION_CREATE_FLASHCARD_SEND.equals(action)))
			{
				mCaller = CALLER_INDICLASH;
			}
		}

		Log.i(AnkiDroidApp.TAG, "CardEditor: caller: " + mCaller);

		// Not sure if following is required
		// if (mCaller == CALLER_INDICLASH &&
		// preferences.getBoolean("intentAdditionInstantAdd", false))

		return getMockNote();
	}

	// Temporary implemented
	private IMultimediaEditableNote getMockNote()
	{
		return MockNoteFactory.makeNote();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.multimedia_delete_note:
				delete();
				return true;

			case R.id.multimedia_save_note:
				save();
				return true;

			case android.R.id.home:

				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{

		if (requestCode == REQUEST_CODE_EDIT_FIELD)
		{

			if (resultCode == RESULT_OK)
			{

				IField field = (IField) data.getSerializableExtra(EditFieldActivity.EXTRA_RESULT_FIELD);
				int index = data.getIntExtra(EditFieldActivity.EXTRA_RESULT_FIELD_INDEX, -1);

				// Failed editing activity
				if (index == -1)
				{
					return;
				}

				getNotePrivate().setField(index, field);
				recreateUi(getNotePrivate());
			}

			super.onActivityResult(requestCode, resultCode, data);
		}

	}

	void save()
	{
		CharSequence text = "Save clicked!";
		showToast(text);
	}

	void delete()
	{
		CharSequence text = "Delete clicked!";
		showToast(text);
	}

	private void showToast(CharSequence text)
	{
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(this, text, duration);
		toast.show();
	}
}
