package com.pfe.predictive.chatbot.dto;

import java.util.List;

public class ChatbotResponse {

    private String answer;
    private boolean authorized;
    private List<String> userRoles;

    public ChatbotResponse() {
    }

    public ChatbotResponse(String answer, boolean authorized, List<String> userRoles) {
        this.answer = answer;
        this.authorized = authorized;
        this.userRoles = userRoles;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public boolean isAuthorized() {
        return authorized;
    }

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }

    public List<String> getUserRoles() {
        return userRoles;
    }

    public void setUserRoles(List<String> userRoles) {
        this.userRoles = userRoles;
    }
}
