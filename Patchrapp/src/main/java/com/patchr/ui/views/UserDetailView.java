package com.patchr.ui.views;

import android.content.Context;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.patchr.BuildConfig;
import com.patchr.R;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.model.RealmEntity;
import com.patchr.objects.CacheStamp;
import com.patchr.model.PhoneNumber;
import com.patchr.ui.LobbyScreen;
import com.patchr.ui.widgets.ImageWidget;
import com.patchr.utilities.UI;

@SuppressWarnings("ucd")
public class UserDetailView extends BaseView {

	private static final Object lock = new Object();

	public    RealmEntity user;
	protected CacheStamp  cacheStamp;
	protected ViewGroup   layout;

	protected Integer  layoutResId   = R.layout.view_profile_header;
	private   Boolean  isCurrentUser = false;

	public ImageWidget          userPhoto;
	public TextView             userName;
	public TextView             authIdentifierLabel;
	public TextView             authIdentifier;
	public TextView             userArea;
	public TextView             buttonMember;
	public TextView             buttonOwner;
	public FloatingActionButton fab;

	public UserDetailView(Context context) {
		this(context, null, 0);
	}

	public UserDetailView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public UserDetailView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initialize();
	}

	public UserDetailView(Context context, Integer layoutResId) {
		super(context, null, 0);
		this.layoutResId = layoutResId;
		initialize();
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override public void invalidate() {
		super.invalidate();
		draw();
	}

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	private void initialize() {

		this.layout = (ViewGroup) LayoutInflater.from(getContext()).inflate(this.layoutResId, this, true);
		ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, ListView.LayoutParams.WRAP_CONTENT);
		this.setLayoutParams(params);

		this.userPhoto = (ImageWidget) layout.findViewById(R.id.user_photo);
		this.userName = (TextView) layout.findViewById(R.id.name);
		this.userArea = (TextView) layout.findViewById(R.id.area);
		this.authIdentifier = (TextView) layout.findViewById(R.id.auth_identifier);
		this.authIdentifierLabel = (TextView) layout.findViewById(R.id.auth_identifier_label);
		this.buttonMember = (TextView) layout.findViewById(R.id.member_of_button);
		this.buttonOwner = (TextView) layout.findViewById(R.id.owner_of_button);
		this.fab = (FloatingActionButton) layout.findViewById(R.id.fab);
	}

	public void draw() {

		if (this.user == null) return;

		this.userPhoto.setImageWithEntity(user.getPhoto(), user.name);
		setOrGone(this.userName, user.name);
		setOrGone(this.userArea, user.area);

		UI.setVisibility(this.authIdentifierLabel, GONE);
		if (this.isCurrentUser) {
			UI.setVisibility(this.authIdentifierLabel, VISIBLE);

			if (BuildConfig.ACCOUNT_KIT_ENABLED) {
				if (UserManager.authTypeHint.equals(LobbyScreen.AuthType.PhoneNumber)) {
					setOrGone(this.authIdentifier, ((PhoneNumber) UserManager.authIdentifierHint).number);
				}
				else {
					setOrGone(this.authIdentifier, (String) UserManager.authIdentifierHint);
				}
			}
			else {
				setOrGone(this.authIdentifier, user.email);
			}
		}

		this.fab.setVisibility(this.isCurrentUser ? VISIBLE : GONE);

		/* Button state */

		Integer watching = user.patchesMember.intValue();
		Integer created = user.patchesOwned.intValue();

		this.buttonMember.setText(StringManager.getString(R.string.label_user_watching)
			+ ": " + ((watching > 0)
			          ? String.valueOf(watching)
			          : StringManager.getString(R.string.label_profile_member_of_empty)));
		this.buttonOwner.setText(StringManager.getString(R.string.label_user_created)
			+ ": " + ((created > 0)
			          ? String.valueOf(created)
			          : StringManager.getString(R.string.label_profile_owner_of_empty)));
	}

	public void bind(RealmEntity user) {
		synchronized (lock) {
			this.user = user;
			this.isCurrentUser = (UserManager.shared().authenticated() && UserManager.currentUser.id.equals(this.user.id));
			draw();
		}
	}
}
