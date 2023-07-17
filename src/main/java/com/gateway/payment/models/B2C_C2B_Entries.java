package com.gateway.payment.models;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.Date;

@Getter
@Setter
public class B2C_C2B_Entries {

    private Long Id;

    private String transactionType;

//    @Indexed(unique = true)
    private String transactionId;

    private String billRefNumber;

    private String msisdn;

    private String amount;

//    @Indexed(unique = true)
    private String conversationId;

//    @Indexed(unique = true)
    private String originatorConversationId;

    private Date EntryDate;

    private String resultCode;

    private Object rawCallbackPayloadResponse;
}