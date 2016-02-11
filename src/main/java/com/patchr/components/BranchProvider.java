package com.patchr.components;

import android.content.Context;

import com.patchr.Patchr;
import com.patchr.objects.Entity;
import com.patchr.objects.User;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.util.LinkProperties;

public class BranchProvider {

	static void logout() {
		Branch.getInstance(Patchr.applicationContext).logout();
	}

	static void setIdentity(String identity) {
		Branch.getInstance(Patchr.applicationContext).setIdentity(identity);
	}
}