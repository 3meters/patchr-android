package com.aircandi.ui.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.aircandi.Patchr;
import com.aircandi.Patchr.ThemeTone;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.FontManager;
import com.aircandi.components.ModelResult;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.StringManager;
import com.aircandi.objects.Category;
import com.aircandi.objects.Photo;
import com.aircandi.objects.TransitionType;
import com.aircandi.ui.base.BaseEdit;
import com.aircandi.interfaces.IBusy.BusyAction;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.UI;

import java.util.List;

public class CategoryBuilder extends BaseEdit {

	private AirImageView mPhotoView;
	private Spinner      mSpinnerCategory;
	private Spinner      mSpinnerSubCategory;
	private Spinner      mSpinnerSubSubCategory;
	private Integer      mSpinnerItem;

	private Category mOriginalCategory;
	private Integer  mOriginalCategoryIndex;
	private Integer  mOriginalSubCategoryIndex;
	private Integer  mOriginalSubSubCategoryIndex;

	private Category mCategory;
	private Category mSubCategory;
	private Category mSubSubCategory;

	private List<Category> mCategories;

	@Override
	public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			final String jsonCategory = extras.getString(Constants.EXTRA_CATEGORY);
			if (jsonCategory != null) {
				mOriginalCategory = (Category) Json.jsonToObject(jsonCategory, Json.ObjectType.CATEGORY);
			}
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mPhotoView = (AirImageView) findViewById(R.id.entity_photo);

		mSpinnerCategory = (Spinner) findViewById(R.id.category);
		mSpinnerSubCategory = (Spinner) findViewById(R.id.sub_category);
		mSpinnerSubSubCategory = (Spinner) findViewById(R.id.sub_sub_category);

		mSpinnerItem = Patchr.themeTone.equals(ThemeTone.DARK) ? R.layout.spinner_item_dark : R.layout.spinner_item_light;
	}

	@Override
	public void bind(BindingMode mode) {

		if (Patchr.getInstance().getEntityManager().getCategories().size() == 0) {
			loadCategories();
		}
		else {
			mCategories = Patchr.getInstance().getEntityManager().getCategories();
			if (mCategories != null) {
				if (mOriginalCategory != null) {
					setCategoryIndexes();
				}
				initCategorySpinner();
			}
		}
		draw(null);
	}

	private void loadCategories() {
		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusy.showBusy(BusyAction.Loading);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("AsyncLoadCategories");
				final ModelResult result = Patchr.getInstance().getEntityManager().loadCategories();
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;
				mBusy.hideBusy(false);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					mCategories = Patchr.getInstance().getEntityManager().getCategories();
					if (mCategories != null) {
						if (mOriginalCategory != null) {
							setCategoryIndexes();
						}
						initCategorySpinner();
					}
				}
			}
		}.execute();
	}

	public void draw(View view) {
		super.draw(view);
		setActivityTitle(StringManager.getString(R.string.dialog_category_builder_title));
	}

	/*--------------------------------------------------------------------------------------------
	 * Events
	 *--------------------------------------------------------------------------------------------*/

	@Override
	public void onAccept() {
		save();
	}

	public void onCancel(Boolean force) {
		setResultCode(Activity.RESULT_CANCELED);
		finish();
		Patchr.getInstance().getAnimationManager().doOverridePendingTransition(this, TransitionType.BUILDER_TO_FORM);
	}

	/*--------------------------------------------------------------------------------------------
	 * Services
	 *--------------------------------------------------------------------------------------------*/

	private void save() {

		final Intent intent = new Intent();
		if (mSubSubCategory != null) {
			final Category category = new Category();
			category.id = mSubSubCategory.id;
			category.name = mSubSubCategory.name;
			category.photo = mSubSubCategory.photo;
			final String jsonCategory = Json.objectToJson(category);
			intent.putExtra(Constants.EXTRA_CATEGORY, jsonCategory);
		}
		else if (mSubCategory != null) {
			final Category category = new Category();
			category.id = mSubCategory.id;
			category.name = mSubCategory.name;
			category.photo = mSubCategory.photo;
			final String jsonCategory = Json.objectToJson(category);
			intent.putExtra(Constants.EXTRA_CATEGORY, jsonCategory);
		}
		else if (mCategory != null) {
			final Category category = new Category();
			category.id = mCategory.id;
			category.name = mCategory.name;
			category.photo = mCategory.photo;
			final String jsonCategory = Json.objectToJson(category);
			intent.putExtra(Constants.EXTRA_CATEGORY, jsonCategory);
		}
		setResultCode(Activity.RESULT_OK, intent);
		finish();
		Patchr.getInstance().getAnimationManager().doOverridePendingTransition(this, TransitionType.BUILDER_TO_FORM);
	}

	/*--------------------------------------------------------------------------------------------
	 * Menus
	 *--------------------------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------------------------
	 * Misc
	 *--------------------------------------------------------------------------------------------*/

	private void setCategoryIndexes() {
		int categoryIndex = 0;
		for (Category category : mCategories) {
			if (category.id.equals(mOriginalCategory.id)) {
				mOriginalCategoryIndex = categoryIndex;
				return;
			}
			else {
				int subcategoryIndex = 0;
				for (Category subcategory : category.categories) {
					if (subcategory.id.equals(mOriginalCategory.id)) {
						mOriginalCategoryIndex = categoryIndex;
						mOriginalSubCategoryIndex = subcategoryIndex;
						return;
					}
					else if (subcategory.categories != null && subcategory.categories.size() > 0) {
						int subsubcategoryIndex = 0;
						for (Category subsubcategory : subcategory.categories) {
							if (subsubcategory.id.equals(mOriginalCategory.id)) {
								mOriginalCategoryIndex = categoryIndex;
								mOriginalSubCategoryIndex = subcategoryIndex;
								mOriginalSubSubCategoryIndex = subsubcategoryIndex;
								return;
							}
							subsubcategoryIndex++;
						}
					}
					subcategoryIndex++;
				}
			}
			categoryIndex++;
		}
	}

	private void initCategorySpinner() {

		final List<String> categories = Patchr.getInstance().getEntityManager().getCategoriesAsStringArray(mCategories);
		final CategoryAdapter adapter = new CategoryAdapter(CategoryBuilder.this
				, mSpinnerItem
				, categories
				, R.string.hint_place_category);

		mSpinnerCategory.setVisibility(View.VISIBLE);
		mSpinnerCategory.setClickable(true);
		mSpinnerCategory.setAdapter(adapter);

		if (mOriginalCategory == null) {
			mSpinnerCategory.setSelection(adapter.getCount());
		}

		mSpinnerCategory.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

				if (position < mCategories.size()) {

					mCategory = mCategories.get(position);
					if (mCategory.photo != null) {
						updateCustomImage(mCategory.photo);
					}

					mSubCategory = null;
					mSubSubCategory = null;
					mSpinnerSubCategory.setVisibility(View.INVISIBLE);
					mSpinnerSubSubCategory.setVisibility(View.INVISIBLE);

					initSubcategorySpinner(position);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		if (mOriginalCategory != null && mOriginalCategoryIndex != null) {
			mSpinnerCategory.setSelection(mOriginalCategoryIndex);
			if (mOriginalSubCategoryIndex == null) {
				mOriginalCategory = null;
			}
		}
	}

	private void initSubcategorySpinner(Integer position) {

		final List<String> categories = Patchr.getInstance().getEntityManager().getCategoriesAsStringArray(mCategory.categories);

		if (categories.size() > 0) {

			final CategoryAdapter adapter = new CategoryAdapter(CategoryBuilder.this
					, mSpinnerItem
					, categories
					, R.string.hint_place_sub_category);

			mSpinnerSubCategory.setVisibility(View.VISIBLE);
			mSpinnerSubCategory.setClickable(true);
			mSpinnerSubCategory.setAdapter(adapter);

			if (mOriginalCategory == null) {
				mSpinnerSubCategory.setSelection(adapter.getCount());
			}

			mSpinnerSubCategory.setOnTouchListener(new OnTouchListener() {

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					if (event.getAction() == MotionEvent.ACTION_UP) {
						if (mSpinnerSubCategory.getSelectedItemPosition() == adapter.getCount()) {
							mSpinnerSubCategory.setSelection(0);
						}
					}
					return false;
				}
			});

			mSpinnerSubCategory.setOnItemSelectedListener(new OnItemSelectedListener() {

				@Override
				public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

					/* Do nothing when the hint item is selected */
					if (position < mCategory.categories.size()) {
						mSubCategory = mCategory.categories.get(position);
						if (mSubCategory.photo != null) {
							updateCustomImage(mSubCategory.photo);
						}

						mSubSubCategory = null;
						mSpinnerSubSubCategory.setVisibility(View.INVISIBLE);

						initSubsubcategorySpinner(position);
					}
				}

				@Override
				public void onNothingSelected(AdapterView<?> parent) {
				}
			});

			if (mOriginalCategory != null && mOriginalSubCategoryIndex != null) {
				mSpinnerSubCategory.setSelection(mOriginalSubCategoryIndex);
				if (mOriginalSubSubCategoryIndex == null) {
					mOriginalCategory = null;
				}
			}
		}
	}

	private void initSubsubcategorySpinner(Integer position) {

		final List<String> categories = Patchr.getInstance().getEntityManager().getCategoriesAsStringArray(mSubCategory.categories);
		if (categories.size() > 0) {

			final CategoryAdapter adapter = new CategoryAdapter(CategoryBuilder.this
					, mSpinnerItem
					, categories
					, R.string.hint_place_sub_category);

			mSpinnerSubSubCategory.setVisibility(View.VISIBLE);
			mSpinnerSubSubCategory.setClickable(true);
			mSpinnerSubSubCategory.setAdapter(adapter);

			if (mOriginalCategory == null) {
				mSpinnerSubSubCategory.setSelection(adapter.getCount());
			}

			mSpinnerSubSubCategory.setOnTouchListener(new OnTouchListener() {

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					if (event.getAction() == MotionEvent.ACTION_UP) {
						if (mSpinnerSubSubCategory.getSelectedItemPosition() == adapter.getCount()) {
							mSpinnerSubSubCategory.setSelection(0);
						}
					}
					return false;
				}
			});

			mSpinnerSubSubCategory.setOnItemSelectedListener(new OnItemSelectedListener() {

				@Override
				public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

					/* Do nothing when the hint item is selected */
					if (position < mSubCategory.categories.size()) {
						mSubSubCategory = mSubCategory.categories.get(position);
						if (mSubSubCategory.photo != null) {
							updateCustomImage(mSubSubCategory.photo);
						}
					}
				}

				@Override
				public void onNothingSelected(AdapterView<?> parent) {
				}
			});

			if (mOriginalCategory != null && mOriginalSubSubCategoryIndex != null) {
				mSpinnerSubSubCategory.setSelection(mOriginalSubSubCategoryIndex);
				mOriginalCategory = null;
			}
		}
	}

	private void updateCustomImage(Photo photo) {
		UI.drawPhoto(mPhotoView, photo);
	}

	private class CategoryAdapter extends ArrayAdapter {

		private final List<String> mCategories;

		private CategoryAdapter(Context context, int textViewResourceId, List categories, Integer categoryHint) {
			super(context, textViewResourceId, categories);
			categories.add(StringManager.getString(categoryHint));
			mCategories = categories;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			final View view = super.getView(position, convertView, parent);
			final TextView text = (TextView) view.findViewById(R.id.spinner_name);
			FontManager.getInstance().setTypefaceLight(text);
			text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);

			if (position == getCount()) {
				text.setText("");
				text.setHint(mCategories.get(getCount())); //"Hint to be displayed"
			}

			return view;
		}

		@Override
		public int getCount() {
			return super.getCount() - 1; // you dont display last item. It is used as hint.
		}
	}

	@Override
	protected int getLayoutId() {
		return R.layout.category_builder;
	}
}