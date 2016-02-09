package com.patchr.objects;

import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.patchr.ui.widgets.AirPhotoView;
import com.patchr.ui.widgets.CandiView;
import com.patchr.ui.widgets.EntityView;
import com.patchr.ui.widgets.UserPhotoView;
import com.patchr.ui.widgets.UserView;

public class ViewHolder {

	public CandiView     candiView;
	public AirPhotoView  photoView;
	public TextView      name;
	public TextView      subhead;
	public TextView      summary;
	public TextView      description;
	public TextView      type;
	public TextView      modifiedDate;
	public TextView      createdDate;
	public UserView      creator;
	public TextView      userName;
	public UserPhotoView userPhotoView;
	public TextView      patchName;
	public AirPhotoView  patchPhotoView;
	public TextView      categoryName;
	public AirPhotoView  categoryPhotoView;
	public TextView      area;
	public EntityView    parent;
	public ViewGroup     share;
	public AirPhotoView  photoViewBig;
	public ImageView     photoType;
	public ImageView     alert;

	public CheckBox checked;
	public Integer  position;        // Used to optimize item view rendering // NO_UCD (unused code)
	public TextView index;

	public String   photoUri;        // Used for verification after fetching image // NO_UCD (unused code)
	public Object   data;            // object binding to
	public TextView comments;
}
