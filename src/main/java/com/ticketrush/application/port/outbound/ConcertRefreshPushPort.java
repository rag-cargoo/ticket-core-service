package com.ticketrush.application.port.outbound;

public interface ConcertRefreshPushPort {

    void sendConcertsRefresh(Long optionId);
}
