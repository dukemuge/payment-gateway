package com.gateway.payment.dtos.mpesa;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class StkPushAsyncResponse{

	@JsonProperty("Body")
	private Body body;
}