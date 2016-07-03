package com.patchr.components;

import android.app.Activity;
import android.support.v4.app.ShareCompat;

import com.patchr.Constants;
import com.patchr.Patchr;
import com.patchr.R;
import com.patchr.model.RealmEntity;
import com.patchr.model.RealmPhoto;
import com.patchr.objects.Photo;
import com.patchr.objects.PhotoCategory;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.util.LinkProperties;

public class BranchProvider {

	public static void logout() {
		Branch.getInstance(Patchr.applicationContext).logout();
	}

	public static void setIdentity(String identity) {
		Branch.getInstance(Patchr.applicationContext).setIdentity(identity);
	}

	public BranchUniversalObject buildApplink(RealmEntity entity) {

		final String patchName = (entity.name != null) ? entity.name : StringManager.getString(R.string.container_singular_lowercase);
		final String referrerName = UserManager.currentUser.name;
		final String referrerId = UserManager.currentUser.id;
		final String ownerName = entity.owner.name;
		final String path = "patch/" + entity.id;

		BranchUniversalObject applink = new BranchUniversalObject()
				.setCanonicalIdentifier(path)
				.setTitle(String.format("Invite by %1$s to the %2$s patch", referrerName, patchName)) // $og_title
				.addContentMetadata("entityId", entity.id)
				.addContentMetadata("entitySchema", Constants.SCHEMA_ENTITY_PATCH)
				.addContentMetadata("referrerName", referrerName)
				.addContentMetadata("referrerId", referrerId)
				.addContentMetadata("ownerName", ownerName)
				.addContentMetadata("patchName", patchName);

		if (UserManager.currentUser.photo != null) {
			Photo photo = UserManager.currentUser.photo;
			applink.addContentMetadata("referrerPhotoUrl", photo.uri(PhotoCategory.PROFILE));
		}

		if (entity.photo != null) {
			RealmPhoto photo = entity.photo;
			String settings = "h=500&crop&fit=crop&q=50";
			String photoUrl = String.format("https://3meters-images.imgix.net/%1$s?%2$s", photo.prefix, settings);
			applink.setContentImageUrl(photoUrl);  // $og_image_url
		}

		if (entity.description != null) {
			applink.setContentDescription(entity.description); // $og_description
		}

		return applink;
	}

	public void invite(final String title, final RealmEntity entity, final Activity activity) {

		final String patchName = (entity.name != null) ? entity.name : StringManager.getString(R.string.container_singular_lowercase);
		final String referrerName = UserManager.currentUser.name;

		BranchUniversalObject applink = buildApplink(entity);

		LinkProperties linkProperties = new LinkProperties()
				.setChannel("patchr-android")
				.setFeature(Branch.FEATURE_TAG_INVITE);

		applink.generateShortUrl(Patchr.applicationContext, linkProperties, new Branch.BranchLinkCreateListener() {

			@Override
			public void onLinkCreate(String url, BranchError error) {

				if (error == null) {
					ShareCompat.IntentBuilder builder = ShareCompat.IntentBuilder.from(activity);
					builder.setChooserTitle(title);
					builder.setType("text/plain");
					/*
					 * subject: Invitation to the \'%1$s\' patch
					 * body: %1$s has invited you to the %2$s patch! %3$s
					 */
					builder.setSubject(String.format(StringManager.getString(R.string.label_patch_share_subject), patchName));
					builder.setText(String.format(StringManager.getString(R.string.label_patch_share_body), referrerName, patchName, url));

					builder.getIntent().putExtra(Constants.EXTRA_SHARE_SOURCE, activity.getPackageName());
					builder.getIntent().putExtra(Constants.EXTRA_SHARE_ID, entity.id);
					builder.getIntent().putExtra(Constants.EXTRA_SHARE_SCHEMA, Constants.SCHEMA_ENTITY_PATCH);

					activity.startActivityForResult(builder.createChooserIntent(), Constants.ACTIVITY_SHARE);
				}
			}
		});
	}

}