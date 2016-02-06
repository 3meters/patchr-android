package com.patchr.components;

import android.content.Context;

import com.patchr.objects.Entity;
import com.patchr.objects.User;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.util.LinkProperties;

/**
 * Created by jaymassena on 2/4/16.
 */
public class BranchProvider {

	public static void invite(Context context, Entity entity, User referrer) {

		BranchUniversalObject branchUniversalObject = new BranchUniversalObject()
				.setCanonicalIdentifier("item/12345")
				.setTitle("My Content Title")
				.setContentDescription("My Content Description")
				.setContentImageUrl("https://example.com/mycontent-12345.png")
				.setContentIndexingMode(BranchUniversalObject.CONTENT_INDEX_MODE.PUBLIC)
				.addContentMetadata("property1", "blue")
				.addContentMetadata("property2", "red");

		LinkProperties linkProperties = new LinkProperties()
				.setChannel("facebook")
				.setFeature("sharing")
				.addControlParameter("$desktop_url", "http://example.com/home")
				.addControlParameter("$ios_url", "http://example.com/ios");

		branchUniversalObject.generateShortUrl(context, linkProperties, new Branch.BranchLinkCreateListener() {
			@Override
			public void onLinkCreate(String url, BranchError error) {
				if (error == null) {
					Logger.i(this, "got my Branch link to share: " + url);
				}
			}
		});
	}

}
