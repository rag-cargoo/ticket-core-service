package com.ticketrush.application.payment.port.outbound;

import java.util.Map;

public interface PaymentMethodConfigPort {

    String getProvider();

    boolean isExternalLiveEnabled();

    Map<String, String> getMethodStatusOverrides();

    Map<String, String> getMethodMessageOverrides();
}
