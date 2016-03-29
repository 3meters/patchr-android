package com.patchr.ui.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.components.StringManager;
import com.patchr.components.UserManager;
import com.patchr.objects.CacheStamp;
import com.patchr.objects.Count;
import com.patchr.objects.Entity;
import com.patchr.objects.Link;
import com.patchr.objects.User;
import com.patchr.ui.widgets.ImageWidget;
import com.patchr.utilities.UI;

@SuppressWarnings("ucd")
public class UserDetailView extends FrameLayout {

	private static final Object lock = new Object();

	public    Entity     entity;
	protected CacheStamp cacheStamp;
	protected ViewGroup  layout;

	protected BaseView base          = new BaseView();
	protected Integer  layoutResId   = R.layout.view_user_header;
	private   Boolean  isCurrentUser = false;

	public ImageWidget          userPhoto;
	public TextView             name;
	public TextView             email;
	public TextView             area;
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

	/*--------------------------------------------------------------------------------------------
	 * Methods
	 *--------------------------------------------------------------------------------------------*/

	private void initialize() {
		this.layout = (ViewGroup) LayoutInflater.from(getContext()).inflate(this.layoutResId, this, true);

		ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, ListView.LayoutParams.WRAP_CONTENT);
		this.setLayoutParams(params);

		this.userPhoto = (ImageWidget) layout.findViewById(R.id.user_photo);
		this.name = (TextView) layout.findViewById(R.id.name);
		this.email = (TextView) layout.findViewById(R.id.email);
		this.area = (TextView) layout.findViewById(R.id.area);
		this.buttonMember = (TextView) layout.findViewById(R.id.member_of_button);
		this.buttonOwner = (TextView) layout.findViewById(R.id.owner_of_button);
		this.fab = (FloatingActionButton) layout.findViewById(R.id.fab);
	}

	public void bind(Entity entity) {

		synchronized (lock) {

			this.entity = entity;
			this.isCurrentUser = UserManager.shared().authenticated()
					&& UserManager.currentUser.id.equals(entity.id);

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

			if (this.isCurrentUser) {
				base.setOrGone(this.email, user.email);
			}

			this.fab.setVisibility(this.isCurrentUser ? VISIBLE : GONE);

			/* Button state */

			Count watching = entity.getCount(Constants.TYPE_LINK_MEMBER, Constants.SCHEMA_ENTITY_PATCH, true, Link.Direction.out);
			Count created = entity.getCount(Constants.TYPE_LINK_CREATE, Constants.SCHEMA_ENTITY_PATCH, true, Link.Direction.out);

			this.buttonMember.setText(StringManager.getString(R.string.label_user_watching)
					+ ": " + ((watching != null)
					          ? String.valueOf(watching.count.intValue())
					          : StringManager.getString(R.string.label_profile_member_of_empty)));
			this.buttonOwner.setText(StringManager.getString(R.string.label_user_created)
					+ ": " + ((created != null)
					          ? String.valueOf(created.count.intValue())
					          : StringManager.getString(R.string.label_profile_owner_of_empty)));
		}
	}
}
