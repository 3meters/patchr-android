package com.patchr.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ViewAnimator;

import com.patchr.R;
import com.patchr.objects.Photo;
import com.patchr.utilities.UI;

@SuppressWarnings("ucd")
public class PhotoEditView extends FrameLayout {

	private static final Object lock = new Object();

	protected Photo    photo;
	protected BaseView base;
	protected Integer  layoutResId;
	protected boolean  collapseIfEmpty;

	protected ViewGroup    layout;
	protected ImageLayout  photoView;
	protected View         setButton;
	protected View         editButton;
	protected View         deleteButton;
	protected ViewAnimator photoAnimator;

	public PhotoEditView(Context context) {
		this(context, null, 0);
	}

	public PhotoEditView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public PhotoEditView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.layoutResId = R.layout.photo_edit_view;
		initialize();
	}

	public PhotoEditView(Context context, Integer layoutResId) {
		super(context, null, 0);
		this.layoutResId = layoutResId;
		initialize();
	}

	protected void initialize() {

		this.base = new BaseView();
		this.layout = (ViewGroup) LayoutInflater.from(getContext()).inflate(this.layoutResId, this, true);

		this.photoView = (ImageLayout) layout.findViewById(R.id.patch_photo);
		this.setButton = (View) layout.findViewById(R.id.photo_set_button);
		this.editButton = (View) layout.findViewById(R.id.photo_edit_button);
		this.deleteButton = (View) layout.findViewById(R.id.photo_delete_button);
		this.photoAnimator = (ViewAnimator) layout.findViewById(R.id.photo_animator);
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	public void databind(Photo photo) {

		synchronized (lock) {

			this.photo = photo;

			UI.setVisibility(setButton, GONE);
			UI.setVisibility(editButton, GONE);
			UI.setVisibility(deleteButton, GONE);

			if (photo == null) {
				if (collapseIfEmpty) {
					photoAnimator.setDisplayedChild(0);
				}
				else {
					photoAnimator.setDisplayedChild(1);
					UI.setVisibility(setButton, VISIBLE);
				}
			}
			else {
				photoAnimator.setDisplayedChild(1);
				photoView.setImageWithPhoto(photo);
				UI.setVisibility(this.editButton, VISIBLE);
				UI.setVisibility(this.deleteButton, VISIBLE);
			}
		}
	}
}