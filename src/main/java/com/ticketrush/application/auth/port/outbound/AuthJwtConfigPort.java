package com.ticketrush.application.auth.port.outbound;

public interface AuthJwtConfigPort {

    String getIssuer();

    String getSecret();

    long getAccessTokenSeconds();

    long getRefreshTokenSeconds();

    String getAccessTokenBlocklistKeyPrefix();
}
