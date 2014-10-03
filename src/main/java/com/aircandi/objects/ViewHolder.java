package com.aircandi.objects;

import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.CandiView;
import com.aircandi.ui.widgets.ComboButton;
import com.aircandi.ui.widgets.EntityView;
import com.aircandi.ui.widgets.UserView;

public class ViewHolder {

	public CandiView    candiView;
	public AirImageView photoView;
	public TextView     name;
	public TextView     subtitle;
	public TextView     description;
	public TextView     type;
	public TextView     createdDate;
	public UserView     creator;
	public TextView     userName;
	public AirImageView userPhotoView;
	public TextView     placeName;
	public TextView     toName;        // NO_UCD (unused code)
	public TextView     area;
	public EntityView   parent;
	public ViewGroup    share;
	public ImageView    alert;

	public ComboButton overflow;
	public CheckBox    checked;
	public Integer     position;        // Used to optimize item view rendering // NO_UCD (unused code)

	public String   photoUri;        // Used for verification after fetching image // NO_UCD (unused code)
	public Object   data;            // object binding to
	public TextView comments;

}
