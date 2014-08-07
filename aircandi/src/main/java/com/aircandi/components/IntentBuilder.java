package com.aircandi.components;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class IntentBuilder extends Extras {

	private Context  mContext;
	private Class<?> mClass;
	private String   mAction;
	private Uri      mData;
	private String   mCategory;
	private String   mMimeType;

	public IntentBuilder() {
	}

	public IntentBuilder(Context context, Class<?> clazz) {
		mContext = context;
		mClass = clazz;
	}

	public IntentBuilder(String action) {
		mAction = action;
	}

	public Intent create() {
		Intent intent = new Intent();

		/* Internal intent */
		if (mContext != null && mClass != null) {
			intent = new Intent(mContext, mClass);
		}

		/* External intent */
		else if (mAction != null) {
			intent = new Intent(mAction);
			if (mData != null) {
				intent.setData(mData);
			}
			if (mMimeType != null) {
				intent.setType(mMimeType);
			}
			if (mCategory != null) {
				intent.addCategory(mCategory);
			}
		}

		if (!mExtras.isEmpty()) {
			intent.putExtras(mExtras);
		}

		if (mData != null) {
			intent.setData(mData);
		}

		return intent;
	}

	public IntentBuilder setData(Uri data) {
		mData = data;
		return this;
	}

	public String getCategory() {
		return mCategory;
	}

	public String getMimeType() {
		return mMimeType;
	}
}
