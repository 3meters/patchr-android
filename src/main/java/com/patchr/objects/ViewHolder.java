package com.patchr.objects;

import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.patchr.ui.views.ImageLayout;
import com.patchr.ui.views.CandiView;
import com.patchr.ui.views.EntityView;
import com.patchr.ui.views.UserView;

public class ViewHolder {

	public CandiView   candiView;
	public ImageLayout photoView;
	public TextView    name;
	public TextView    subhead;
	public TextView    summary;
	public TextView    description;
	public TextView    type;
	public TextView    modifiedDate;
	public TextView    createdDate;
	public UserView    creator;
	public TextView    userName;
	public ImageLayout userPhotoView;
	public TextView    patchName;
	public ImageLayout patchPhotoView;
	public TextView    categoryName;
	public ImageLayout categoryPhotoView;
	public TextView    area;
	public EntityView  parent;
	public ViewGroup   share;
	public ImageLayout photoViewBig;
	public ImageView   photoType;
	public ImageView   alert;

	public CheckBox checked;
	public Integer  position;        // Used to optimize item view rendering // NO_UCD (unused code)
	public TextView index;

	public String   photoUri;        // Used for verification after fetching image // NO_UCD (unused code)
	public Object   data;            // object binding to
	public TextView comments;
}
