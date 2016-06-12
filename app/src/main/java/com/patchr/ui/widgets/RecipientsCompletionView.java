package com.patchr.ui.widgets;

import android.content.Context;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.patchr.R;
import com.patchr.objects.Recipient;
import com.tokenautocomplete.TokenCompleteTextView;

public class RecipientsCompletionView extends TokenCompleteTextView<Recipient> {

	public RecipientsCompletionView(Context context) {
		super(context);
	}

	public RecipientsCompletionView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public RecipientsCompletionView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override protected View getViewForObject(Recipient object) {
		TextView view = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.view_token, (ViewGroup) RecipientsCompletionView.this.getParent(), false);
		view.setText(object.name);
		return view;
	}

	@Override protected Recipient defaultObject(String completionText) {
		return null;
	}

	public void deleteText() {
		Editable text = getText();
		text.replace(text.length() - currentCompletionText().length(), text.length(), "");
	}

	public String currentCompletionText() {
		return super.currentCompletionText();
	}
}
