package com.patchr.components;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import com.kbeanie.imagechooser.api.BChooserPreferences;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.utilities.Booleans;

import java.io.File;
import java.io.FileOutputStream;

@SuppressWarnings("ucd")
public class MediaManager {

	public static Integer SOUND_PLACES_FOUND;
	public static Integer SOUND_ACTIVITY_NEW;
	public static Integer SOUND_DEBUG_POP;
	public static Integer SOUND_ACTIVITY_CHANGE;

	public static AudioManager audioManager;
	public static SoundPool    soundPool;
	public static boolean      loaded;
	public static  Integer streamType        = AudioManager.STREAM_SYSTEM;
	public static  String  tempDirectoryName = ".Patchr";

	static {
		/* Called first time a static member is accessed */
		//noinspection deprecation
		soundPool = new SoundPool(4, streamType, 0); // New SoundPool.Builder requires API 21
		soundPool.setOnLoadCompleteListener((soundPool1, sampleId, status) -> loaded = true);

		audioManager = (AudioManager) Patchr.applicationContext.getSystemService(Context.AUDIO_SERVICE);

		SOUND_ACTIVITY_NEW = soundPool.load(Patchr.applicationContext, R.raw.onesignal_default_sound, 1);
		SOUND_PLACES_FOUND = soundPool.load(Patchr.applicationContext, R.raw.notification_candi_discovered_soft, 1);
		SOUND_DEBUG_POP = soundPool.load(Patchr.applicationContext, R.raw.notification_pop, 1);
		SOUND_ACTIVITY_CHANGE = soundPool.load(Patchr.applicationContext, R.raw.notification_carme, 1);

		/* Set the folder used by image chooser for all files */
		BChooserPreferences preferences = new BChooserPreferences(Patchr.applicationContext);
		preferences.setFolderName("Pictures/Patchr");
	}

	public static void playSound(Integer soundResId, Float multiplier, Integer loops) {
		if (soundPool != null && loaded) {
			if (Patchr.settings.getBoolean(StringManager.getString(R.string.pref_sound_effects)
				, Booleans.getBoolean(R.bool.pref_sound_effects_default))) {

				/* Getting the user sound settings */
				float actualVolume = (float) audioManager.getStreamVolume(streamType);
				float maxVolume = (float) audioManager.getStreamMaxVolume(streamType);
				float volume = actualVolume / maxVolume;

				soundPool.play(soundResId, volume, volume, 1, (loops - 1), 1f);
			}
		}
	}

	@SuppressWarnings("EmptyMethod") public static void warmup() {}

	public static Boolean canCaptureWithCamera() {
		return AndroidManager.isIntentAvailable(MediaStore.ACTION_IMAGE_CAPTURE)
			&& Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
	}

	public static void scanMedia(File file) {
		Uri uri = Uri.fromFile(file);
		scanMedia(uri);
	}

	public static void scanMedia(Uri uri) {
		Intent scanFileIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
		Patchr.applicationContext.sendBroadcast(scanFileIntent);
	}

	public static String getTempDirectory(String directoryName) {

		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			File directory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + directoryName);
			if (!directory.exists()) {
				//noinspection ResultOfMethodCallIgnored
				directory.mkdirs();
			}
			return directory.getAbsolutePath();
		}
		return null;
	}

	public static String getPhotoDirectory() {

		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			File photoDir = new File(Environment.DIRECTORY_PICTURES, StringManager.getString(R.string.name_app));
			return photoDir.toString();
		}
		return null;
	}

	public static String getSharePath() {
		String directory = getTempDirectory(tempDirectoryName);
		if (directory != null) {
			String shareFileName = "photo.jpeg";
			return directory + File.separator + shareFileName;
		}
		return null;
	}

	public static Uri getSharePathUri() {
		String sharePath = getSharePath();
		if (sharePath != null) {
			return Uri.parse("file://" + sharePath);
		}
		return null;
	}

	public static File copyBitmapToSharePath(Bitmap bitmap) {

		File file = null;
		String path = getSharePath();
		if (path == null) return null;

		try {
			file = new File(getSharePath());
			FileOutputStream outputStream = new FileOutputStream(file);
			bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream);
			outputStream.flush();
			outputStream.close();
		}
		catch (Exception e) {
			ReportingManager.logException(e);
		}
		return file;
	}

	public static boolean copyBitmapToInternalStorage(Context context, Bitmap bitmap, String filename) {

		try {
			FileOutputStream outputStream = context.openFileOutput(filename, Context.MODE_PRIVATE);
			bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
			outputStream.flush();
			outputStream.close();
			return true;
		}
		catch (Exception e) {
			ReportingManager.logException(e);
		}
		return false;
	}
}