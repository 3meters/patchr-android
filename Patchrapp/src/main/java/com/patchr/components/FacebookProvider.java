package com.patchr.components;

import android.app.Activity;
import android.net.Uri;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.share.model.AppInviteContent;
import com.facebook.share.widget.AppInviteDialog;
import com.patchr.R;
import com.patchr.model.Photo;
import com.patchr.model.RealmEntity;
import com.patchr.objects.AnalyticsCategory;
import com.patchr.objects.PhotoCategory;
import com.patchr.utilities.Reporting;
import com.patchr.utilities.UI;
import com.patchr.utilities.Utils;
import com.segment.analytics.Properties;

public class FacebookProvider {
	public void invite(final String title, RealmEntity entity, Activity activity, CallbackManager callbackManager) {

		AppInviteDialog inviteDialog = new AppInviteDialog(activity);

		if (AppInviteDialog.canShow()) {

			String patchName = (entity.name != null) ? entity.name : StringManager.getString(R.string.container_singular_lowercase);
			String patchPhotoUrl = null;
			String referrerNameEncoded = Utils.encode(UserManager.currentUser.name);
			String referrerPhotoUrl = "";

			if (UserManager.currentUser.getPhoto() != null) {
				Photo photo = UserManager.currentUser.getPhoto();
				String photoUrlEncoded = Utils.encode(photo.uri(PhotoCategory.PROFILE));
				referrerPhotoUrl = String.format("&referrerPhotoUrl=%1$s", photoUrlEncoded);
			}

			String queryString = String.format("entityId=%1$s&entitySchema=%2$s&referrerName=%3$s%4$s", entity.id, entity.schema, referrerNameEncoded, referrerPhotoUrl);
			Uri applink = Uri.parse(String.format("https://fb.me/934234473291708?%1$s", queryString));

			if (entity.getPhoto() != null) {
				Photo photo = entity.getPhoto();
				String patchNameEncoded = Utils.encode(patchName);
				String settings = "w=1200&h=628&crop&fit=crop&q=25&txtsize=96&txtalign=left,bottom&txtcolor=fff&txtshad=5&txtpad=60&txtfont=Helvetica%20Neue%20Light";
				patchPhotoUrl = String.format("https://3meters-images.imgix.net/%1$s?%2$s&txt=%3$s", photo.prefix, settings, patchNameEncoded);
			}

			AppInviteContent.Builder builder = new AppInviteContent.Builder();
			builder.setApplinkUrl(applink.toString());
			if (patchPhotoUrl != null) {
				builder.setPreviewImageUrl(patchPhotoUrl);
			}

			inviteDialog.registerCallback(callbackManager, new FacebookCallback<AppInviteDialog.Result>() {

				@Override
				public void onSuccess(AppInviteDialog.Result result) {
					Reporting.track(AnalyticsCategory.EDIT, "Sent Patch Invitation", new Properties().putValue("network", "Facebook"));
					UI.toast("Facebook invites sent");
				}

				@Override
				public void onCancel() {
					UI.toast("Facebook invite cancelled");
				}

				@Override
				public void onError(FacebookException error) {
					Logger.w(this, String.format("Facebook invite error: %1$s", error.toString()));
				}
			});
			AppInviteDialog.show(activity, builder.build());
		}
	}
}
