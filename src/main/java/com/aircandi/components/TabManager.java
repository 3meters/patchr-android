package com.aircandi.components;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.widget.ViewFlipper;

import com.aircandi.Constants;
import com.aircandi.R;

import java.util.Locale;

public class TabManager implements ActionBar.TabListener {

	private ActionBar   mActionBar;
	private ViewFlipper mViewFlipper;
	private Integer     mTabsProfileId;

	public TabManager() {
	}

	public TabManager(Integer tabsProfileId, ActionBar actionBar, ViewFlipper viewFlipper) {
		mTabsProfileId = tabsProfileId;
		mActionBar = actionBar;
		mViewFlipper = viewFlipper;
	}

	public void initialize() {
		addTabsToActionBar(this, getTabsProfileId());
	}

	private void addTabsToActionBar(ActionBar.TabListener tabListener, int tabsId) {
		mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		if (tabsId == Constants.TABS_ENTITY_FORM_ID) {

			ActionBar.Tab tab = mActionBar.newTab();
			tab.setText(StringManager.getString(R.string.label_tab_item_content).toUpperCase(Locale.US));
			tab.setTag(R.string.label_tab_item_content);
			tab.setTabListener(tabListener);
			mActionBar.addTab(tab, false);

			tab = mActionBar.newTab();
			tab.setText(StringManager.getString(R.string.label_tab_item_settings).toUpperCase(Locale.US));
			tab.setTag(R.string.label_tab_item_settings);
			tab.setTabListener(tabListener);
			mActionBar.addTab(tab, false);
		}
		else if (tabsId == Constants.TABS_USER_EDIT_ID) {

			ActionBar.Tab tab = mActionBar.newTab();
			tab.setText(StringManager.getString(R.string.label_tab_item_profile).toUpperCase(Locale.US));
			tab.setTag(R.string.label_tab_item_profile);
			tab.setTabListener(tabListener);
			mActionBar.addTab(tab, false);

			tab = mActionBar.newTab();
			tab.setText(StringManager.getString(R.string.label_tab_item_account).toUpperCase(Locale.US));
			tab.setTag(R.string.label_tab_item_account);
			tab.setTabListener(tabListener);
			mActionBar.addTab(tab, false);
		}
	}

	public void setActiveTab(int position) {
		if (mActionBar.getTabCount() == 0) return;
		if ((mActionBar.getSelectedTab() == null
				|| mActionBar.getSelectedTab().getPosition() != position)
				&& mActionBar.getTabCount() >= (position - 1)) {
			mActionBar.getTabAt(position).select();
		}
	}

	@Override
	public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
		Logger.v(this, "onTabSelected: " + tab.getTag());
		/* Currently handles tab switching in all forms with view flippers */
		if (mViewFlipper != null) {
			mViewFlipper.setDisplayedChild(tab.getPosition());
		}
	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
	}

	public void doSaveInstanceState(Bundle savedInstanceState) {
		/*
		 * This gets called from comment, profile and entity forms
		 */
		if (mActionBar != null && mActionBar.getTabCount() > 0) {
			savedInstanceState.putInt("tab_index", (mActionBar.getSelectedTab() != null) ? mActionBar.getSelectedTab().getPosition() : 0);
		}
	}

	public void doRestoreInstanceState(Bundle savedInstanceState) {
		/*
		 * This gets everytime Common is created and savedInstanceState bundle is
		 * passed to the constructor.
		 * 
		 * This gets called from comment, profile and entity forms
		 */
		if (savedInstanceState != null) {
			if (mActionBar != null && mActionBar.getTabCount() > 0) {
				setActiveTab(savedInstanceState.getInt("tab_index"));
			}
		}
	}

	public ActionBar getActionBar() {
		return mActionBar;
	}

	public void setActionBar(ActionBar actionBar) {
		mActionBar = actionBar;
	}

	public ViewFlipper getViewFlipper() {
		return mViewFlipper;
	}

	public void setViewFlipper(ViewFlipper viewFlipper) {
		mViewFlipper = viewFlipper;
	}

	public Integer getTabsProfileId() {
		return mTabsProfileId;
	}

	public void setTabsProfileId(Integer tabsProfileId) {
		mTabsProfileId = tabsProfileId;
	}
}