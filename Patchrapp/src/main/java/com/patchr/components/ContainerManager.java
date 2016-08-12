package com.patchr.components;

import com.amazonaws.auth.BasicAWSCredentials;
import com.google.android.gms.tagmanager.Container;
import com.google.android.gms.tagmanager.ContainerHolder;
import com.patchr.utilities.UI;
import com.patchr.utilities.Utils;

public class ContainerManager implements ContainerHolder.ContainerAvailableListener {

	private static ContainerManager instance = new ContainerManager();
	private static ContainerHolder containerHolder;

	/* Container values */
	public static  String AWS_ACCESS_KEY             = "aws-access-key";
	public static  String AWS_SECRET_KEY             = "aws-secret-key";
	public static  String BING_SUBSCRIPTION_KEY      = "bing-subscription-key";
	public static  String USER_SECRET                = "user-secret";
	private static String CREATIVE_SDK_CLIENT_SECRET = "creative-sdk-client-secret";

	public static ContainerManager getInstance() {
		return instance;
	}

	private ContainerManager() {}

	public Container getContainer() {
		return containerHolder.getContainer();
	}

	public void setContainerHolder(ContainerHolder holder) {
		containerHolder = holder;
		containerHolder.setContainerAvailableListener(this);
		registerCallbacksForContainer(containerHolder.getContainer());
	}

	@Override public void onContainerAvailable(ContainerHolder containerHolder, String s) {

		/* Called when the initial container or a new version of the container become available */
		Container container = containerHolder.getContainer();   // Makes the container active
		registerCallbacksForContainer(container);

		String message = "Container activated using default";

		if (!container.isDefault()) {
			message = "Container activated using non-default";
			String accessKey = container.getString(AWS_ACCESS_KEY);
			String secretKey = container.getString(AWS_SECRET_KEY);
			S3.awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
		}

		Logger.v(this, message);
		if (Utils.devModeEnabled()) {
			UI.toast(message);
		}
	}

	public static void registerCallbacksForContainer(Container container) {}

	public void refresh() {
		containerHolder.refresh();
	}
}
