package com.pfe.predictive.alert.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "alert.notifications")
public class AlertNotificationProperties {

	private boolean enabled;
	private List<String> criticalAlertRecipients = List.of();

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public List<String> getCriticalAlertRecipients() {
		return criticalAlertRecipients;
	}

	public void setCriticalAlertRecipients(List<String> criticalAlertRecipients) {
		this.criticalAlertRecipients = criticalAlertRecipients;
	}
}