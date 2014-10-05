package com.aircandi.components;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import com.aircandi.Patch;
import com.aircandi.R;
import com.aircandi.utilities.Booleans;
import com.aircandi.utilities.Reporting;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

@SuppressWarnings("ucd")
public class MediaManager {

	public static Integer SOUND_PLACES_FOUND;
	public static Integer SOUND_ACTIVITY_NEW;
	public static Integer SOUND_DEBUG_POP;
	public static Integer SOUND_ACTIVITY_CHANGE;

	public static SoundPool soundPool;
	public static Integer streamType = AudioManager.STREAM_NOTIFICATION;
	public static AudioManager audioManager;

	/*
	 * Sharing Management
	 * ------------------
	 * 
	 * MessageEdit: If intent has image stream we pull the bitmap from it and copy it to our
	 * pinned share file. The uri string to the file is used as the photo.prefix. Later when
	 * the message is stored, the bitmap is loaded and sent to S3.
	 * 
	 * PhotoForm: If user shares photo we copy to pinned share file and pass uri pointer to
	 * the app shared to.
	 * 
	 * MessageForm: If user shares message with photo, we copy the photo to pinned share file
	 * and pass uri pointer to the app shared to.
	 * 
	 * PlaceForm: If user shares patch with photo, we copy the photo to pinned share file
	 * and pass uri pointer to the app shared to.
	 * 
	 * Photo Management
	 * ------------------
	 * 
	 * This covers photos that are created using our application. If a user selects a photo
	 * from for instance the Instragram album, we don't want to create a copy in Pictures/Candigram.
	 * 
	 * We call ImageChooser and pass in the folder to use for storage. We want to use our standard
	 * temp/share folder for photos choosen that already exist. We use Pictures/Candipatch for photos
	 * created using the camera.
	 */

	private static String shareFileName     = "photo.jpeg";
	public static  String tempDirectoryName = ".Patchr";

	public MediaManager initSoundPool() {
		soundPool = new SoundPool(4, streamType, 100);
		audioManager = (AudioManager) Patch.applicationContext.getSystemService(Context.AUDIO_SERVICE);

		SOUND_ACTIVITY_NEW = soundPool.load(Patch.applicationContext, R.raw.notification_activity, 1);
		SOUND_PLACES_FOUND = soundPool.load(Patch.applicationContext, R.raw.notification_candi_discovered_soft, 1);
		SOUND_DEBUG_POP = soundPool.load(Patch.applicationContext, R.raw.notification_pop, 1);
		SOUND_ACTIVITY_CHANGE = soundPool.load(Patch.applicationContext, R.raw.notification_carme, 1);
		return this;
	}

	public static void playSound(Integer soundResId, Float multiplier, Integer loops) {
		if (soundPool != null) {
			if (Patch.settings.getBoolean(StringManager.getString(R.string.pref_sound_effects), Booleans.getBoolean(R.bool.pref_sound_effects_default))) {

				/* Getting the user sound settings */
				float actualVolume = (float) audioManager.getStreamVolume(streamType);
				float maxVolume = (float) audioManager.getStreamMaxVolume(streamType);
				float volume = actualVolume / maxVolume;

				soundPool.play(soundResId, volume, volume, 1, (loops - 1), 1f);
			}
		}
	}

	public static Boolean canCaptureWithCamera() {
		return AndroidManager.isIntentAvailable(Patch.applicationContext, MediaStore.ACTION_IMAGE_CAPTURE)
				&& Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
	}

	public static void scanMedia(File file) {
		Uri uri = Uri.fromFile(file);
		scanMedia(uri);
	}

	public static void scanMedia(Uri uri) {
		Intent scanFileIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
		Patch.applicationContext.sendBroadcast(scanFileIntent);
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
			return directory + File.separator + shareFileName;
		}
		return null;
	}

	public static Uri getSharePathUri() {
		String sharePath = getSharePath();
		if (sharePath != null) {
			return Uri.parse("file:" + sharePath);
		}
		return null;
	}

	public static File copyBitmapToSharePath(Bitmap bitmap) {

		File file = null;
		try {
			file = new File(getSharePath());
			FileOutputStream outputStream = new FileOutputStream(file);
			bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream);
			outputStream.flush();
			outputStream.close();
		}
		catch (FileNotFoundException e) {
			Reporting.logException(e);
		}
		catch (IOException e) {
			Reporting.logException(e);
		}
		catch (Exception e) {
			Reporting.logException(e);
		}
		return file;
	}
}
