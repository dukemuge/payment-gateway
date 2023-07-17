package com.gateway.payment.services;



import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.payment.configurations.MpesaConfig;
import com.gateway.payment.dtos.mpesa.*;
import com.gateway.payment.utils.Constants;
import com.gateway.payment.utils.HelperUtility;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.Objects;

import static com.gateway.payment.utils.Constants.AUTHORIZATION_HEADER_STRING;
import static com.gateway.payment.utils.Constants.*;


@Service
@Slf4j

public class DarajaApiImpl implements DarajaApi {

    private final MpesaConfig mpesaConfig;
    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;


    public DarajaApiImpl(MpesaConfig mpesaConfig, OkHttpClient okHttpClient, ObjectMapper objectMapper) {
        this.mpesaConfig = mpesaConfig;
        this.okHttpClient = okHttpClient;
        this.objectMapper = objectMapper;
    }

    /**
     * @return Returns Daraja API Access Token Response
     */
    @Override
    public AccessTokenResponse getAccessToken() {

        // get the Base64 rep of consumerKey + ":" + consumerSecret
        String encodedCredentials = HelperUtility.toBase64String(String.format("%s:%s", mpesaConfiguration.getConsumerKey(),
                mpesaConfig.getConsumerSecret()));

        Request request = new Request.Builder()
                .url(String.format("%s?grant_type=%s", mpesaConfig.getOauthEndpoint(), mpesaConfiguration.getGrantType()))
                .get()
                .addHeader(AUTHORIZATION_HEADER_STRING, String.format("%s %s", BASIC_AUTH_STRING, encodedCredentials))
                .addHeader(CACHE_CONTROL_HEADER, CACHE_CONTROL_HEADER_VALUE)
                .build();

        try {
            Response response = okHttpClient.newCall(request).execute();
            assert response.body() != null;

            // use Jackson to Decode the ResponseBody ...
            return objectMapper.readValue(response.body().string(), AccessTokenResponse.class);
        } catch (IOException e) {
            log.error(String.format("Could not get access token. -> %s", e.getLocalizedMessage()));
            return null;
        }
    }

    @Override
    public RegisterUrlResponse registerUrl() {
        AccessTokenResponse accessTokenResponse = getAccessToken();

        RegisterUrlRequest registerUrlRequest = new RegisterUrlRequest();
        registerUrlRequest.setConfirmationURL(mpesaConfig.getConfirmationURL());
        registerUrlRequest.setResponseType(mpesaConfig.getResponseType());
        registerUrlRequest.setShortCode(mpesaConfig.getShortCode());
        registerUrlRequest.setValidationURL(mpesaConfig.getValidationURL());


        RequestBody body = RequestBody.create(JSON_MEDIA_TYPE,
                Objects.requireNonNull(HelperUtility.toJson(registerUrlRequest)));

        Request request = new Request.Builder()
                .url(mpesaConfiguration.getRegisterUrlEndpoint())
                .post(body)
                .addHeader("Authorization", String.format("%s %s", BEARER_AUTH_STRING, accessTokenResponse.getAccessToken()))
                .build();

        try {
            Response response = okHttpClient.newCall(request).execute();

            assert response.body() != null;
            // use Jackson to Decode the ResponseBody ...
            return objectMapper.readValue(response.body().string(), RegisterUrlResponse.class);

        } catch (IOException e) {
            log.error(String.format("Could not register url -> %s", e.getLocalizedMessage()));
            return null;
        }
    }

    @Override
    public SimulateTransactionResponse simulateC2BTransaction(SimulateTransactionRequest simulateTransactionRequest) {
        AccessTokenResponse accessTokenResponse = getAccessToken();
        log.info(String.format("Access Token: %s", accessTokenResponse.getAccessToken()));
        RequestBody body = RequestBody.create(JSON_MEDIA_TYPE,
                Objects.requireNonNull(HelperUtility.toJson(simulateTransactionRequest)));

        Request request = new Request.Builder()
                .url(mpesaConfig.getSimulateTransactionEndpoint())
                .post(body)
                .addHeader(AUTHORIZATION_HEADER_STRING, String.format("%s %s", BEARER_AUTH_STRING, accessTokenResponse.getAccessToken()))
                .build();

        try {
            Response response = okHttpClient.newCall(request).execute();
            assert response.body() != null;
            // use Jackson to Decode the ResponseBody ...

            return objectMapper.readValue(response.body().string(), SimulateTransactionResponse.class);
        } catch (IOException e) {
            log.error(String.format("Could not simulate C2B transaction -> %s", e.getLocalizedMessage()));
            return null;
        }

    }

    @Override
    public CommonSyncResponse performB2CTransaction(InternalB2CTransactionRequest internalB2CTransactionRequest) {
        AccessTokenResponse accessTokenResponse = getAccessToken();
        log.info(String.format("Access Token: %s", accessTokenResponse.getAccessToken()));

        B2CTransactionRequest b2CTransactionRequest = new B2CTransactionRequest();

        b2CTransactionRequest.setCommandID(internalB2CTransactionRequest.getCommandID());
        b2CTransactionRequest.setAmount(internalB2CTransactionRequest.getAmount());
        b2CTransactionRequest.setPartyB(internalB2CTransactionRequest.getPartyB());
        b2CTransactionRequest.setRemarks(internalB2CTransactionRequest.getRemarks());
        b2CTransactionRequest.setOccassion(internalB2CTransactionRequest.getOccassion());

        // get the security credentials ...
        b2CTransactionRequest.setSecurityCredential(HelperUtility.getSecurityCredentials(mpesaConfig.getB2cInitiatorPassword()));

        log.info(String.format("Security Creds: %s", b2CTransactionRequest.getSecurityCredential()));

        // set the result url ...
        b2CTransactionRequest.setResultURL(mpesaConfig.getB2cResultUrl());
        b2CTransactionRequest.setQueueTimeOutURL(mpesaConfig.getB2cQueueTimeoutUrl());
        b2CTransactionRequest.setInitiatorName(mpesaConfig.getB2cInitiatorName());
        b2CTransactionRequest.setPartyA(mpesaConfig.getShortCode());

        RequestBody body = RequestBody.create(JSON_MEDIA_TYPE,
                Objects.requireNonNull(HelperUtility.toJson(b2CTransactionRequest)));

        Request request = new Request.Builder()
                .url(mpesaConfig.getB2cTransactionEndpoint())
                .post(body)
                .addHeader(AUTHORIZATION_HEADER_STRING, String.format("%s %s", BEARER_AUTH_STRING, accessTokenResponse.getAccessToken()))
                .build();

        try {
            Response response = okHttpClient.newCall(request).execute();

            assert response.body() != null;

            return objectMapper.readValue(response.body().string(), CommonSyncResponse.class);
        } catch (IOException e) {
            log.error(String.format("Could not perform B2C transaction ->%s", e.getLocalizedMessage()));
            return null;
        }

    }

    @Override
    public TransactionStatusSyncResponse getTransactionResult(InternalTransactionStatusRequest internalTransactionStatusRequest) {

        TransactionStatusRequest transactionStatusRequest = new TransactionStatusRequest();
        transactionStatusRequest.setTransactionID(internalTransactionStatusRequest.getTransactionID());

        transactionStatusRequest.setInitiator(mpesaConfig.getB2cInitiatorName());
        transactionStatusRequest.setSecurityCredential(HelperUtility.getSecurityCredentials(mpesaConfig.getB2cInitiatorPassword()));
        transactionStatusRequest.setCommandID(TRANSACTION_STATUS_QUERY_COMMAND);
        transactionStatusRequest.setPartyA(mpesaConfig.getShortCode());
        transactionStatusRequest.setIdentifierType(SHORT_CODE_IDENTIFIER);
        transactionStatusRequest.setResultURL(mpesaConfig.getB2cResultUrl());
        transactionStatusRequest.setQueueTimeOutURL(mpesaConfig.getB2cQueueTimeoutUrl());
        transactionStatusRequest.setRemarks(TRANSACTION_STATUS_VALUE);
        transactionStatusRequest.setOccasion(TRANSACTION_STATUS_VALUE);

        AccessTokenResponse accessTokenResponse = getAccessToken();

        RequestBody body = RequestBody.create(JSON_MEDIA_TYPE,
                Objects.requireNonNull(HelperUtility.toJson(transactionStatusRequest)));

        Request request = new Request.Builder()
                .url(mpesaConfig.getTransactionResultUrl())
                .post(body)
                .addHeader(AUTHORIZATION_HEADER_STRING, String.format("%s %s", BEARER_AUTH_STRING, accessTokenResponse.getAccessToken()))
                .build();

        try {
            Response response = okHttpClient.newCall(request).execute();
            assert response.body() != null;
            // use Jackson to Decode the ResponseBody ...

            return objectMapper.readValue(response.body().string(), TransactionStatusSyncResponse.class);
        } catch (IOException e) {
            log.error(String.format("Could not fetch transaction result -> %s", e.getLocalizedMessage()));
            return null;
        }


    }

    @Override
    public CommonSyncResponse checkAccountBalance() {

        CheckAccountBalanceRequest checkAccountBalanceRequest = new CheckAccountBalanceRequest();
        checkAccountBalanceRequest.setInitiator(mpesaConfig.getB2cInitiatorName());
        checkAccountBalanceRequest.setSecurityCredential(HelperUtility.getSecurityCredentials(mpesaConfiguration.getB2cInitiatorPassword()));
        checkAccountBalanceRequest.setCommandID(Constants.ACCOUNT_BALANCE_COMMAND);
        checkAccountBalanceRequest.setPartyA(mpesaConfig.getShortCode());
        checkAccountBalanceRequest.setIdentifierType(Constants.SHORT_CODE_IDENTIFIER);
        checkAccountBalanceRequest.setRemarks("Check Account Balance");
        checkAccountBalanceRequest.setQueueTimeOutURL(mpesaConfig.getB2cQueueTimeoutUrl());
        checkAccountBalanceRequest.setResultURL(mpesaConfig.getB2cResultUrl());

        AccessTokenResponse accessTokenResponse = getAccessToken();

        RequestBody body = RequestBody.create(JSON_MEDIA_TYPE,
                Objects.requireNonNull(HelperUtility.toJson(checkAccountBalanceRequest)));

        Request request = new Request.Builder()
                .url(mpesaConfig.getCheckAccountBalanceUrl())
                .post(body)
                .addHeader(AUTHORIZATION_HEADER_STRING, String.format("%s %s", BEARER_AUTH_STRING, accessTokenResponse.getAccessToken()))
                .build();

        try {
            Response response = okHttpClient.newCall(request).execute();
            assert response.body() != null;
            // use Jackson to Decode the ResponseBody ...

            return objectMapper.readValue(response.body().string(), CommonSyncResponse.class);
        } catch (IOException e) {
            log.error(String.format("Could not fetch the account balance -> %s", e.getLocalizedMessage()));
            return null;
        }

    }

    @Override
    public StkPushSyncResponse performStkPushTransaction(InternalStkPushRequest internalStkPushRequest) {

        ExternalStkPushRequest externalStkPushRequest = new ExternalStkPushRequest();
        externalStkPushRequest.setBusinessShortCode(mpesaConfig.getStkPushShortCode());

        String transactionTimestamp = HelperUtility.getTransactionTimestamp();
        String stkPushPassword = HelperUtility.getStkPushPassword(mpesaConfig.getStkPushShortCode(),
                mpesaConfig.getStkPassKey(), transactionTimestamp);

        externalStkPushRequest.setPassword(stkPushPassword);
        externalStkPushRequest.setTimestamp(transactionTimestamp);
        externalStkPushRequest.setTransactionType(Constants.CUSTOMER_PAYBILL_ONLINE);
        externalStkPushRequest.setAmount(internalStkPushRequest.getAmount());
        externalStkPushRequest.setPartyA(internalStkPushRequest.getPhoneNumber());
        externalStkPushRequest.setPartyB(mpesaConfig.getStkPushShortCode());
        externalStkPushRequest.setPhoneNumber(internalStkPushRequest.getPhoneNumber());
        externalStkPushRequest.setCallBackURL(mpesaConfig.getStkPushRequestCallbackUrl());
        externalStkPushRequest.setAccountReference(HelperUtility.getTransactionUniqueNumber());
        externalStkPushRequest.setTransactionDesc(String.format("%s Transaction", internalStkPushRequest.getPhoneNumber()));

        AccessTokenResponse accessTokenResponse = getAccessToken();

        RequestBody body = RequestBody.create(JSON_MEDIA_TYPE,
                Objects.requireNonNull(HelperUtility.toJson(externalStkPushRequest)));

        Request request = new Request.Builder()
                .url(mpesaConfig.getStkPushRequestUrl())
                .post(body)
                .addHeader(AUTHORIZATION_HEADER_STRING, String.format("%s %s", BEARER_AUTH_STRING, accessTokenResponse.getAccessToken()))
                .build();


        try {
            Response response = okHttpClient.newCall(request).execute();
            assert response.body() != null;
            // use Jackson to Decode the ResponseBody ...

            return objectMapper.readValue(response.body().string(), StkPushSyncResponse.class);
        } catch (IOException e) {
            log.error(String.format("Could not perform the STK push request -> %s", e.getLocalizedMessage()));
            return null;
        }

    }

    @Override
    public LNMQueryResponse getTransactionStatus(InternalLNMRequest internalLNMRequest) {

        ExternalLNMQueryRequest externalLNMQueryRequest = new ExternalLNMQueryRequest();
        externalLNMQueryRequest.setBusinessShortCode(mpesaConfig.getStkPushShortCode());

        String requestTimestamp = HelperUtility.getTransactionTimestamp();
        String stkPushPassword = HelperUtility.getStkPushPassword(mpesaConfig.getStkPushShortCode(),
                mpesaConfig.getStkPassKey(), requestTimestamp);

        externalLNMQueryRequest.setPassword(stkPushPassword);
        externalLNMQueryRequest.setTimestamp(requestTimestamp);
        externalLNMQueryRequest.setCheckoutRequestID(internalLNMRequest.getCheckoutRequestID());

        AccessTokenResponse accessTokenResponse = getAccessToken();

        RequestBody body = RequestBody.create(JSON_MEDIA_TYPE,
                Objects.requireNonNull(HelperUtility.toJson(externalLNMQueryRequest)));

        Request request = new Request.Builder()
                .url(mpesaConfig.getLnmQueryRequestUrl())
                .post(body)
                .addHeader(AUTHORIZATION_HEADER_STRING, String.format("%s %s", BEARER_AUTH_STRING, accessTokenResponse.getAccessToken()))
                .build();

        try {
            Response response = okHttpClient.newCall(request).execute();
            assert response.body() != null;
            // use Jackson to Deserialising the ResponseBody ...
            return objectMapper.readValue(response.body().string(), LNMQueryResponse.class);
        } catch (IOException e) {
            log.error(String.format("Could not get the transaction status -> %s", e.getLocalizedMessage()));
            return null;
        }

    }
}
