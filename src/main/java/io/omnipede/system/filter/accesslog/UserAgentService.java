package io.omnipede.system.filter.accesslog;

import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;

/**
 * User agent 분석 시 사용하는 클래스
 */
class UserAgentService {

    private final UserAgentAnalyzer uaa;

    public UserAgentService() {
        uaa = UserAgentAnalyzer
                .newBuilder()
                .withField("DeviceClass")
                .withCache(25000)
                .build();
    }

    /**
     * User-Agent 헤더로부터 device class 를 추출하는 메소드
     * @param userAgent User-Agent header value
     * @return Device class
     */
    public String getDeviceClass(String userAgent) {
        if (userAgent == null || userAgent.length() == 0)
            return null;

        UserAgent parsedUserAgent = uaa.parse(userAgent);
        return parsedUserAgent.getValue("DeviceClass");
    }
}
