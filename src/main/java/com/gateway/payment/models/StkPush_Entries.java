package com.gateway.payment.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class StkPush_Entries {

    private String internalId;

    private String transactionId;

    private String transactionType;

    private String msisdn;

    private BigDecimal amount;

    private String merchantRequestID;

    private String checkoutRequestID;

    private LocalDate entryDate;

    private String resultCode;

    private String rawCallbackPayloadResponse;

}