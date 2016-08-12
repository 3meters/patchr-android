package com.patchr.ui.views;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.SwitchCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.patchr.Constants;
import com.patchr.R;
import com.patchr.components.MediaManager;
import com.patchr.components.UserManager;
import com.patchr.model.Link;
import com.patchr.model.RealmEntity;
import com.patchr.objects.enums.AnalyticsCategory;
import com.patchr.objects.enums.UserRole;
import com.patchr.service.RestClient;
import com.patchr.ui.widgets.ImageWidget;
import com.patchr.utilities.Errors;
import com.patchr.components.ReportingManager;
import com.patchr.utilities.UI;

import io.realm.Realm;

@SuppressWarnings("ucd")
public class UserView extends BaseView implements View.OnClickListener {

	private static final Object lock = new Object();

	public    RealmEntity entity;
	public    RealmEntity patch;
	protected Integer     layoutResId;

	public Boolean showEmail = false;
	public boolean processing;

	protected ViewGroup    layout;
	private   ImageWidget  userPhoto;
	private   TextView     nameView;
	private   TextView     emailView;
	private   TextView     areaView;
	private   TextView     roleView;
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
		layoutResId = R.layout.view_user;
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
		if (!processing) {
			processing = true;
			Boolean approved = ((CompoundButton) view).isChecked();
			enableLabel.setText(approved ? R.string.label_watcher_enabled : R.string.label_watcher_not_enabled);
			Link link = entity.getLink();
			approveMember(entity, link.id, approved);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	private void initialize() {

		layout = (ViewGroup) LayoutInflater.from(getContext()).inflate(layoutResId, this, true);

		ListView.LayoutParams params = new ListView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		setLayoutParams(params);

		userPhoto = (ImageWidget) layout.findViewById(R.id.user_photo);
		nameView = (TextView) layout.findViewById(R.id.name);
		emailView = (TextView) layout.findViewById(R.id.email);
		areaView = (TextView) layout.findViewById(R.id.area);
		roleView = (TextView) layout.findViewById(R.id.role);
		editGroup = (ViewGroup) layout.findViewById(R.id.edit_group);
		enableSwitch = (SwitchCompat) layout.findViewById(R.id.enable_switch);
		enableLabel = (TextView) layout.findViewById(R.id.enable_label);
		removeButton = (ImageButton) layout.findViewById(R.id.remove_button);
		if (enableSwitch != null) {
			enableSwitch.setOnClickListener(this);
		}
	}

	public void bind(RealmEntity entity) {
		bind(entity, null);
	}

	public void bind(RealmEntity entity, RealmEntity patch) {

		synchronized (lock) {

			this.entity = entity;

			UI.setVisibility(emailView, GONE);
			UI.setVisibility(roleView, GONE);
			UI.setVisibility(editGroup, GONE);

			userPhoto.setImageWithEntity(entity, null);
			setOrGone(nameView, entity.name);
			setOrGone(areaView, entity.area);

			if (showEmail) {
				setOrGone(emailView, entity.email);
			}

			Link link = entity.getLink();
			if (patch != null && link != null) {
				this.patch = patch;
				if (entity.id.equals(patch.ownerId)) {
					setOrGone(roleView, UserRole.OWNER);
				}
				else if (!patch.visibility.equals(Constants.PRIVACY_PUBLIC) && UserManager.currentUser != null && UserManager.userId.equals(patch.ownerId)) {
					removeButton.setTag(entity);
					enableSwitch.setChecked(link.enabled);
					enableLabel.setText(link.enabled ? R.string.label_watcher_enabled : R.string.label_watcher_not_enabled);
					UI.setVisibility(editGroup, VISIBLE);
				}
			}
		}
	}

	public void approveMember(final RealmEntity entity, final String linkId, final Boolean enabled) {

		String entityId = entity.id;

		RestClient.getInstance().enableLinkById(linkId, enabled)
			.map(response -> {
				Realm realm = Realm.getDefaultInstance();
				realm.executeTransaction(whocares -> {
					RealmEntity realmEntity = realm.where(RealmEntity.class).equalTo("id", entityId).findFirst();
					if (realmEntity != null) {
						Link link = realmEntity.getLink();
						link.enabled = enabled;
						realmEntity.setLink(link);
					}
				});
				realm.close();
				return response;
			})
			.subscribe(
				response -> {
					processing = false;
					if (!((Activity) getContext()).isFinishing()) {
						ReportingManager.getInstance().track(AnalyticsCategory.ACTION, enabled ? "Approved Member" : "Unapproved Member");
						MediaManager.playSound(MediaManager.SOUND_DEBUG_POP, 1.0f, 1);
					}
				},
				error -> {
					processing = false;
					if (!((Activity) getContext()).isFinishing()) {
						Errors.handleError(getContext(), error);
					}
				});
	}
}
