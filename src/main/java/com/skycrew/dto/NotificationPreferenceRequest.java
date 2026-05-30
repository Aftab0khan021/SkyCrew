package com.skycrew.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationPreferenceRequest {

    private boolean emailEnabled;
    private boolean smsEnabled;
    private String phoneNumber;
}
