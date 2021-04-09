package com.coinplug.system.filter.accesslog;

import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;


/**
 * Access log 를 남기는 필터.
 *
 * @author 서현규
 */
public class AccessLogFilter extends OncePerRequestFilter {

    // Access log filter configuration
    private AccessLogFilterConfigurer accessLogFilterConfigurer;

    // User agent 분석 시 사용하는 객체
    private UserAgentService userAgentService = new UserAgentService();

    // 실제 access log 를 남길 시 사용하는 인터페이스
    private AccessLogger accessLogger;

    // 서버 host name
    private HostName hostName = new HostName();

    public AccessLogFilter(AccessLogFilterConfigurer accessLogFilterConfigurer) {
        this.accessLogFilterConfigurer = accessLogFilterConfigurer;
        this.accessLogger = new DefaultAccessLogger();
    }

    public AccessLogFilter(AccessLogFilterConfigurer accessLogFilterConfigurer, AccessLogger accessLogger) {
        this.accessLogFilterConfigurer = accessLogFilterConfigurer;
        this.accessLogger = accessLogger;
    }

    /**
     * Normal servlet filter method.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {

        // White list 된 request URI 일 경우 pass 처리
        if (isWhiteListed(httpServletRequest.getRequestURI())) {
            filterChain.doFilter(httpServletRequest, httpServletResponse);
            return;
        }

        // Access log 처리
        processAccessLog(httpServletRequest, httpServletResponse, filterChain);
    }

    /**
     * 요청 URI 가 white list 된 URI 인지 확인하는 메소드
     * @param requestUri 요청 URI
     * @return White list 여부
     */
    private boolean isWhiteListed(String requestUri) {
        // 설정 로드
        List<String> whiteList = accessLogFilterConfigurer.getWhiteList();
        if (whiteList == null)
            return false;

        // White list 상에서 request uri 가 존재하는지 확인
        String findResult = whiteList
                .stream().filter(requestUri::startsWith)
                .findFirst()
                .orElse(null);

        // 존재하면 true 반환
        return findResult != null;
    }

    /**
     * Access log 를 남기는 메소드
     */
    private void processAccessLog(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws IOException, ServletException {

        // IF enableContentCapture is true
        // request, response body 를 로그에 남김
        if (accessLogFilterConfigurer.isEnableContentLogging()) {
            processAccessLogWithContents(httpServletRequest, httpServletResponse, filterChain);
            return;
        }

        // Else, request response body 를 로그에 남기지 않음
        processAccessLogWithoutContents(httpServletRequest, httpServletResponse, filterChain);
    }

    /**
     * Contents (request, response body) 를 로그에 남기는 메소드
     */
    private void processAccessLogWithContents(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws IOException, ServletException {
        CachingRequestWrapper requestWrapper = new CachingRequestWrapper(httpServletRequest);
        CachingResponseWrapper responseWrapper = new CachingResponseWrapper(httpServletResponse);
        // 요청 시각
        Date requestAt = new Date();
        filterChain.doFilter(requestWrapper, responseWrapper);
        // 응답 시각
        Date responseAt = new Date();
        AccessLog accessLog = createAccessLog(requestWrapper, responseWrapper, requestAt, responseAt);

        // Body 추출
        String requestBody = getRequestBody(requestWrapper);
        String responseBody = getResponseBody(responseWrapper);

        // Access 로그에 body 추가
        accessLog.setRequestBody(requestBody);
        accessLog.setResponseBody(responseBody);

        // 로그 남기기
        accessLogger.log(accessLog);
    }

    /**
     * Contents (request, response body) 를 로그에 남기지 않음
     */
    private void processAccessLogWithoutContents(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws IOException, ServletException {
        // 요청 시각
        Date requestAt = new Date();
        filterChain.doFilter(httpServletRequest, httpServletResponse);
        // 응답 시각
        Date responseAt = new Date();
        AccessLog accessLog = createAccessLog(httpServletRequest, httpServletResponse, requestAt, responseAt);
        // 로그 남기기
        accessLogger.log(accessLog);
    }

    /**
     * Servlet request, servlet response 상에서 로깅할 정보를 추출하는 메소드
     * @param httpServletRequest Servlet request
     * @param httpServletResponse Servlet response
     * @param requestAt 요청 시각
     * @param responseAt 응답 시각
     */
    private AccessLog createAccessLog(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Date requestAt, Date responseAt) {
        // 소요 시간 측정
        long elapsed = responseAt.getTime() - requestAt.getTime();

        // User-Agent 분석
        String userAgent = httpServletRequest.getHeader("User-Agent");
        if (userAgent == null)
            userAgent = "Unknown";
        String deviceClass = userAgentService.getDeviceClass(userAgent);

        // Request
        // IP address
        String ip = extractIpAddress(httpServletRequest);
        // URI
        String uri = httpServletRequest.getRequestURI();
        // URL query part
        String query = httpServletRequest.getQueryString();

        // METHOD
        String method = httpServletRequest.getMethod();
        // Request headers
        Map<String, String> requestHeaders = getMapOfRequestHeaders(httpServletRequest);
        requestHeaders.put("DeviceClass", deviceClass);
        // Response STATUS
        Integer httpStatus = httpServletResponse.getStatus();
        // Response headers
        Map<String, String> responseHeaders = getMapOfResponseHeaders(httpServletResponse);

        return AccessLog.builder()
                .requestAt(requestAt)
                .responseAt(responseAt)
                .userAgent(userAgent)
                // Add hostname
                .hostName(hostName.toString())
                .ip(ip)
                .uri(uri)
                .query(query)
                .method(method)
                .requestHeaders(requestHeaders)
                .status(httpStatus)
                .responseHeaders(responseHeaders)
                // Add elapsed time
                .elapsed(elapsed)
                .build();
    }

    /**
     * Servlet request 의 헤더에서 IP 주소를 추출하는 메소드
     * @param httpServletRequest IP 주소를 추출할 servlet request
     * @return IP 주소
     */
    private String extractIpAddress(HttpServletRequest httpServletRequest) {

        // 요청이 proxy 되었을 때 다음 헤더 중 하나에 원본 client ip 가 존재한다.

        String xForwardedFor = httpServletRequest.getHeader("X-Forwarded-For");
        if (isValidIp(xForwardedFor))
            return xForwardedFor;

        String proxyClientIp = httpServletRequest.getHeader("Proxy-Client-IP");
        if (isValidIp(proxyClientIp))
            return proxyClientIp;

        String wlProxyClientIp = httpServletRequest.getHeader("WL-Proxy-Client-IP");
        if (isValidIp(wlProxyClientIp))
            return wlProxyClientIp;

        String httpClientIp = httpServletRequest.getHeader("HTTP_CLIENT_IP");
        if (isValidIp(httpClientIp))
            return httpClientIp;

        String httpXForwardedFor = httpServletRequest.getHeader("HTTP_X_FORWARDED_FOR");
        if (isValidIp(httpXForwardedFor))
            return httpXForwardedFor;

        // 그 외의 경우, proxy 되지 않았다고 판단하여 getRemoteAddr() 메소드로 IP 주소를 가져옴.
        return httpServletRequest.getRemoteAddr();
    }

    /**
     * 대상 IP 문자열이 valid ip string 인지 확인하는 메소드
     * @param target 확인할 문자열
     * @return IP address validity
     */
    private boolean isValidIp(String target) {
        if (target == null || target.length() == 0)
            return false;

        return !"unknown".equalsIgnoreCase(target);
    }

    /**
     * Servlet request 의 헤더를 Map 형태로 변환하는 메소드
     * @param httpServletRequest 헤더를 포함하는 servlet request
     * @return Map 형태의 헤더
     */
    private Map<String, String> getMapOfRequestHeaders(HttpServletRequest httpServletRequest) {
        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> headerNames = httpServletRequest.getHeaderNames();
        while(headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, httpServletRequest.getHeader(headerName));
        }
        return headers;
    }

    /**
     * Servlet response 의 헤더를 Map 형태로 변환하는 메소드
     * @param httpServletResponse 헤더를 포함하는 servlet response
     * @return Map 형태의 헤더
     */
    private Map<String, String> getMapOfResponseHeaders(HttpServletResponse httpServletResponse) {
        Map<String, String> headers = new LinkedHashMap<>();
        httpServletResponse.getHeaderNames().forEach((headerName) -> {
            headers.put(headerName, httpServletResponse.getHeader(headerName));
        });
        return headers;
    }

    /**
     * 요청 바디 추출
     * @param cachingRequestWrapper 요청 바디를 추출할 wrapper 객체
     * @return 요청 바디
     */
    private String getRequestBody(CachingRequestWrapper cachingRequestWrapper) {
        byte[] buf = cachingRequestWrapper.getContentsAsByteArray();
        // 최대 길이보다 긴지 확인하고 반환
        if (buf.length > accessLogFilterConfigurer.getMaxContentLength())
            return "TOO LONG CONTENTS";

        return new String(buf, StandardCharsets.UTF_8)
                .replaceAll("[\\n\\t]", "");
    }

    /**
     * 응답 바디 추출
     * @param cachingResponseWrapper 응답 바디 추출할 wrapper 객체
     * @return 응답 바디
     */
    private String getResponseBody(CachingResponseWrapper cachingResponseWrapper) {
        byte[] buf = cachingResponseWrapper.getContentAsByteArray();
        // 최대 길이보다 긴지 확인하고 반환
        if (buf.length > accessLogFilterConfigurer.getMaxContentLength())
            return "TOO LONG CONTENTS";

        return new String(buf, StandardCharsets.UTF_8)
                .replaceAll("[\\n\\t]", "");
    }
}
