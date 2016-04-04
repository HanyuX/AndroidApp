package edu.dartmouth.cs.camera;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.soundcloud.android.crop.Crop;

import edu.dartmouth.cs.camera.R;
// extra credit -- take from camera or gallery

public class CameraControlActivity extends Activity {

	public static final int REQUEST_CODE_TAKE_FROM_CAMERA = 0;
	//public static final int REQUEST_CODE_CROP_PHOTO = 2;

	private static final String IMAGE_UNSPECIFIED = "image/*";
	private static final String URI_INSTANCE_STATE_KEY = "saved_uri";

	private Uri mImageCaptureUri;
	private ImageView mImageView;
	private boolean isTakenFromCamera;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.profile);
		mImageView = (ImageView) findViewById(R.id.imageProfile);

		if (savedInstanceState != null) {
			mImageCaptureUri = savedInstanceState
					.getParcelable(URI_INSTANCE_STATE_KEY);
			mImageView.setImageURI(mImageCaptureUri);
			if(mImageCaptureUri == null) {
				loadSnap();
				loadProfile();
			}
		}
		else {
			loadSnap();
			loadProfile();
		}
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		// Save the image capture uri before the activity goes into background
		outState.putParcelable(URI_INSTANCE_STATE_KEY, mImageCaptureUri);
	}

	// ****************** button click callbacks ***************************//

	public void onSaveClicked(View v) {
		// Save picture
		saveSnap();
		// Save Profile
		saveProfile();

		// Close the activity
		finish();
	}

	public void onCancleClicked(View v) {
		Toast.makeText(getApplicationContext(), "Cancled",
				Toast.LENGTH_SHORT).show();
		finish();
	}

	public void onChangePhotoClicked(View v) {
		// changing the profile image, show the dialog asking the user
		// to choose between taking a picture
		// Go to MyRunsDialogFragment for details.
		displayDialog(MyRunsDialogFragment.DIALOG_ID_PHOTO_PICKER);
	}

	// Handle data after activity returns.
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != RESULT_OK)
			return;

		switch (requestCode) {
		case REQUEST_CODE_TAKE_FROM_CAMERA:
			// Send image taken from camera for cropping
			beginCrop(mImageCaptureUri);
			break;

		case Crop.REQUEST_CROP: //We changed the RequestCode to the one being used by the library.
			// Update image view after image crop
			handleCrop(resultCode, data);

			// Delete temporary image taken by camera after crop.
			if (isTakenFromCamera) {
				File f = new File(mImageCaptureUri.getPath());
				if (f.exists())
					f.delete();
			}

			break;
		}
	}

	// ******* Photo picker dialog related functions ************//

	public void displayDialog(int id) {
		DialogFragment fragment = MyRunsDialogFragment.newInstance(id);
		fragment.show(getFragmentManager(),
				getString(R.string.dialog_fragment_tag_photo_picker));
	}

	public void onPhotoPickerItemSelected(int item) {
		Intent intent;

		switch (item) {

		case MyRunsDialogFragment.ID_PHOTO_PICKER_FROM_CAMERA:
			// Take photo from camera，
			// Construct an intent with action
			// MediaStore.ACTION_IMAGE_CAPTURE
			intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			// Construct temporary image path and name to save the taken
			// photo
			mImageCaptureUri = Uri.fromFile(new File(Environment
					.getExternalStorageDirectory(), "tmp_"
					+ String.valueOf(System.currentTimeMillis()) + ".jpg"));
			intent.putExtra(MediaStore.EXTRA_OUTPUT,
					mImageCaptureUri);
			intent.putExtra("return-data", true);
			try {
				// Start a camera capturing activity
				// REQUEST_CODE_TAKE_FROM_CAMERA is an integer tag you
				// defined to identify the activity in onActivityResult()
				// when it returns
				startActivityForResult(intent, REQUEST_CODE_TAKE_FROM_CAMERA);
			} catch (ActivityNotFoundException e) {
				e.printStackTrace();
			}
			isTakenFromCamera = true;
			break;
			
		default:
			return;
		}

	}

	// ****************** private helper functions ***************************//

	private void loadSnap() {


		// Load profile photo from internal storage
		try {
			FileInputStream fis = openFileInput(getString(R.string.profile_photo_file_name));
			Bitmap bmap = BitmapFactory.decodeStream(fis);
			mImageView.setImageBitmap(bmap);
			fis.close();
		} catch (IOException e) {
			// Default profile photo if no photo saved before.
			mImageView.setImageResource(R.drawable.default_profile);
		}
	}

	private void saveSnap() {

	// Commit all the changes into preference file
		// Save profile image into internal storage.
		mImageView.buildDrawingCache();
		Bitmap bmap = mImageView.getDrawingCache();
		try {
			FileOutputStream fos = openFileOutput(
					getString(R.string.profile_photo_file_name), MODE_PRIVATE);
			bmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
			fos.flush();
			fos.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	/** Method to start Crop activity using the library
	 *	Earlier the code used to start a new intent to crop the image,
	 *	but here the library is handling the creation of an Intent, so you don't
	 * have to.
	 *  **/
	private void beginCrop(Uri source) {
		Uri destination = Uri.fromFile(new File(getCacheDir(), "cropped"));
		Crop.of(source, destination).asSquare().start(this);
	}

	private void handleCrop(int resultCode, Intent result) {
		if (resultCode == RESULT_OK) {
			mImageView.setImageURI(Crop.getOutput(result));
		} else if (resultCode == Crop.RESULT_ERROR) {
			Toast.makeText(this, Crop.getError(result).getMessage(), Toast.LENGTH_SHORT).show();
		}
	}

	private void loadProfile() {
		String mKey = getString(R.string.preference_name);
		SharedPreferences mPrefs = getSharedPreferences(mKey, MODE_PRIVATE);

		Log.d("ccc", "load");
		// Load Name
		mKey = getString(R.string.preference_key_profile_name);
		String mValue = mPrefs.getString(mKey, " ");
		if(mValue != " ")
			((EditText) findViewById(R.id.profileName)).setText(mValue);

		// Load Email
		mKey = getString(R.string.preference_key_profile_email);
		mValue = mPrefs.getString(mKey, " ");
		if(mValue != " ")
			((EditText) findViewById(R.id.profileEmail)).setText(mValue);

		// Load Phone
		mKey = getString(R.string.preference_key_profile_phone);
		mValue = mPrefs.getString(mKey, " ");
		if(mValue != " ")
			((EditText) findViewById(R.id.profilePhone)).setText(mValue);

		// Load Class
		mKey = getString(R.string.preference_key_profile_class);
		mValue = mPrefs.getString(mKey, " ");
		if(mValue != " ")
			((EditText) findViewById(R.id.profileClass)).setText(mValue);

		// Load Major
		mKey = getString(R.string.preference_key_profile_major);
		mValue = mPrefs.getString(mKey, " ");
		if(mValue != " ")
			((EditText) findViewById(R.id.profileMajor)).setText(mValue);

		// Load Gender
		mKey = getString(R.string.preference_key_profile_gender);
		int mIntValue = mPrefs.getInt(mKey, -1);
		// In case there isn't one saved before:
		if (mIntValue >= 0) {
			// Find the radio button that should be checked.
			RadioButton radioBtn = (RadioButton) ((RadioGroup) findViewById(R.id.profileGender))
					.getChildAt(mIntValue);
			// Check the button.
			radioBtn.setChecked(true);
		}
	}

	private void saveProfile() {
		// Getting the shared preferences editor

		String mKey = getString(R.string.preference_name);
		SharedPreferences mPrefs = getSharedPreferences(mKey, MODE_PRIVATE);

		SharedPreferences.Editor mEditor = mPrefs.edit();
		mEditor.clear();

		//Add Name
		mKey = getString(R.string.preference_key_profile_name);
		String mNameValue = (String) ((EditText) findViewById(R.id.profileName))
				.getText().toString();
		mEditor.putString(mKey, mNameValue);

		//Add Email
		mKey = getString(R.string.preference_key_profile_email);
		String mValue = (String) ((EditText) findViewById(R.id.profileEmail))
				.getText().toString();
		mEditor.putString(mKey, mValue);

		//Add Phone
		mKey = getString(R.string.preference_key_profile_phone);
		mValue = (String) ((EditText) findViewById(R.id.profilePhone))
				.getText().toString();
		mEditor.putString(mKey, mValue);

		//Add Gender
		mKey = getString(R.string.preference_key_profile_gender);
		RadioGroup mRadioGroup = (RadioGroup) findViewById(R.id.profileGender);
		int mIntValue = mRadioGroup.indexOfChild(findViewById(mRadioGroup
				.getCheckedRadioButtonId()));
		mEditor.putInt(mKey, mIntValue);

		//Add Class
		mKey = getString(R.string.preference_key_profile_class);
		mValue = (String) ((EditText) findViewById(R.id.profileClass))
				.getText().toString();
		mEditor.putString(mKey, mValue);

		//Add Major
		mKey = getString(R.string.preference_key_profile_major);
		mValue = (String) ((EditText) findViewById(R.id.profileMajor))
				.getText().toString();
		mEditor.putString(mKey, mValue);

		// Commit all the changes into the shared preference
		mEditor.commit();

		Toast.makeText(getApplicationContext(), "Saved Profile Of: " + mNameValue,
				Toast.LENGTH_SHORT).show();
	}
}