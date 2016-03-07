package com.patchr.components;

import com.patchr.Patchr;

import io.branch.referral.Branch;

public class BranchProvider {

	static void logout() {
		Branch.getInstance(Patchr.applicationContext).logout();
	}

	static void setIdentity(String identity) {
		Branch.getInstance(Patchr.applicationContext).setIdentity(identity);
	}
}