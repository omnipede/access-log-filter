package io.omnipede.system.filter.accesslog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

import java.util.Date;
import java.util.Map;
import java.util.Optional;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class AccessLog {

    // 요청 시각
    Date requestAt;
    // 응답 시각
    Date responseAt;

    // Client hostname
    private String userAgent;
    private String hostName;
    private String ip;
    private String uri;
    private String query;
    private String method;
    private Map<String, String> requestHeaders;
    @Nullable
    private String requestBody;

    // Response status
    private Integer status;

    // Response headers
    private Map<String, String> responseHeaders;
    @Nullable
    private String responseBody;

    // 요청 처리 소요 시간
    private long elapsed;
}
