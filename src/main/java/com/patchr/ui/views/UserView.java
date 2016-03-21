package com.patchr.ui.views;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.support.v7.widget.SwitchCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.DataController;
import com.patchr.components.ModelResult;
import com.patchr.components.NetworkManager;
import com.patchr.objects.CacheStamp;
import com.patchr.objects.Entity;
import com.patchr.objects.Shortcut;
import com.patchr.objects.User;
import com.patchr.utilities.Errors;
import com.patchr.utilities.UI;

@SuppressWarnings("ucd")
public class UserView extends FrameLayout implements View.OnClickListener {

	private static final Object lock = new Object();

	public    Entity     entity;
	protected CacheStamp cacheStamp;
	public    Entity     patch;
	protected BaseView   base;
	protected Integer    layoutResId;

	public Boolean showEmail = false;

	protected ViewGroup    layout;
	private   ImageLayout  userPhoto;
	private   TextView     name;
	private   TextView     email;
	private   TextView     area;
	private   TextView     role;
	private   ViewGroup    editGroup;
	private   ImageButton  removeButton;
	private   SwitchCompat enableSwitch;
	private   TextView     enableLabel;

	public UserView(Context context) {
		this(context, null, 0);
	}

	public UserView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public UserView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.layoutResId = R.layout.user_view;
		initialize();
	}

	public UserView(Context context, Integer layoutResId) {
		super(context, null, 0);
		this.layoutResId = layoutResId;
		initialize();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public void onClick(View view) {
		if (view.getId() == R.id.enable_switch) {
			onApprovedClick(view);
		}
	}

	public void onApprovedClick(View view) {
		Boolean approved = ((CompoundButton) view).isChecked();
		this.enableLabel.setText(approved ? R.string.label_watcher_enabled : R.string.label_watcher_not_enabled);
		approveMember(this.entity, this.entity.linkId, this.entity.id, this.patch.id, approved);
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	private void initialize() {

		this.base = new BaseView();
		this.layout = (ViewGroup) LayoutInflater.from(getContext()).inflate(this.layoutResId, this, true);

		ListView.LayoutParams params = new ListView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		this.setLayoutParams(params);

		this.userPhoto = (ImageLayout) layout.findViewById(R.id.user_photo);
		this.name = (TextView) layout.findViewById(R.id.name);
		this.email = (TextView) layout.findViewById(R.id.email);
		this.area = (TextView) layout.findViewById(R.id.area);
		this.role = (TextView) layout.findViewById(R.id.role);
		this.editGroup = (ViewGroup) layout.findViewById(R.id.edit_group);
		this.enableSwitch = (SwitchCompat) layout.findViewById(R.id.enable_switch);
		this.enableLabel = (TextView) layout.findViewById(R.id.enable_label);
		this.removeButton = (ImageButton) layout.findViewById(R.id.remove_button);
		if (this.enableSwitch != null) {
			this.enableSwitch.setOnClickListener(this);
		}
	}

	public void databind(Entity entity) {
		databind(entity, false, false);
	}

	public void databind(Entity entity, Boolean enableEdit, Boolean asOwner) {

		synchronized (lock) {

			this.entity = entity;

			UI.setVisibility(this.email, GONE);
			UI.setVisibility(this.role, GONE);
			UI.setVisibility(this.editGroup, GONE);

			if (entity == null) {
				Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.img_default_user_light);
				final BitmapDrawable bitmapDrawable = new BitmapDrawable(Patchr.applicationContext.getResources(), bitmap);
				UI.showDrawableInImageView(bitmapDrawable, userPhoto.imageView, Constants.ANIMATE_IMAGES);
				this.name.setText("Guest");
				this.area.setText(null);
				return;
			}

			this.cacheStamp = entity.getCacheStamp();
			User user = (User) entity;

			this.userPhoto.setImageWithEntity(user);
			base.setOrGone(this.name, user.name);
			base.setOrGone(this.area, user.area);

			if (this.showEmail) {
				base.setOrGone(this.email, user.email);
			}

			if (asOwner) {
				base.setOrGone(this.role, User.Role.OWNER);
			}

			if (enableEdit && !asOwner) {
				this.removeButton.setTag(entity);
				this.enableSwitch.setChecked(entity.linkEnabled);
				this.enableLabel.setText(entity.linkEnabled ? R.string.label_watcher_enabled : R.string.label_watcher_not_enabled);
				UI.setVisibility(this.editGroup, VISIBLE);
			}
		}
	}

	public void approveMember(final Entity entity, final String linkId, final String fromId, final String toId, final Boolean enabled) {

		final String actionEvent = (enabled ? "approve" : "unapprove") + "_watch_entity";
		final Shortcut toShortcut = new Shortcut();
		toShortcut.schema = Constants.SCHEMA_ENTITY_PATCH;

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncStatusUpdate");
				return DataController.getInstance().insertLink(linkId
						, fromId
						, toId
						, Constants.TYPE_LINK_MEMBER
						, enabled
						, toShortcut, actionEvent, true, NetworkManager.SERVICE_GROUP_TAG_DEFAULT, null
				);
			}

			@Override
			protected void onPostExecute(Object response) {
				if (((Activity) getContext()).isFinishing()) return;
				ModelResult result = (ModelResult) response;

				if (result.serviceResponse.responseCode == NetworkManager.ResponseCode.SUCCESS) {
					entity.linkEnabled = enabled;
				}
				else {
					Errors.handleError((Activity) getContext(), result.serviceResponse);
				}
			}
		}.executeOnExecutor(Constants.EXECUTOR);
	}
}
