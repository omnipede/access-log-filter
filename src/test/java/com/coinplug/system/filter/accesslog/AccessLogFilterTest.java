package com.coinplug.system.filter.accesslog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.FilterChain;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Access log 필터 테스트
 */
@ExtendWith(MockitoExtension.class)
class AccessLogFilterTest {

    // 필터 설정
    private AccessLogFilterConfigurer accessLogFilterConfigurer;

    @Mock
    private AccessLogger accessLogger;

    @Mock
    private FilterChain filterChain;

    /**
     * Content 를 로깅하는 테스트
     */
    @Test
    public void test_content_logging() throws Exception {

        // Given
        MockHttpServletRequest httpServletRequest = givenMockHttpServletRequest();
        MockHttpServletResponse httpServletResponse = givenMockHttpServletResponse();

        accessLogFilterConfigurer = AccessLogFilterConfigurer
                .builder()
                .enableContentLogging(true)
                .maxContentLength(1024 * 1024)
                .build();

        accessLogger = accessLog -> {
            // AccessLog 객체 내부에 정의된 필드가 존재하는지 확인한다.
            assertThat(accessLog.getRequestAt()).isNotNull();
            assertThat(accessLog.getResponseAt()).isNotNull();
            assertThat(accessLog.getUserAgent()).isNotNull();
            assertThat(accessLog.getHostName()).isNotNull();
            assertThat(accessLog.getIp()).isNotNull();
            assertThat(accessLog.getUri()).isEqualTo("/api/v1/foo/bar");
            assertThat(accessLog.getQuery()).isEqualTo("?hello=world");
            assertThat(accessLog.getMethod()).isEqualToIgnoringCase("post");
            assertThat(accessLog.getRequestHeaders().get("SAMPLE-HEADER")).isEqualTo("12345");
            assertThat(accessLog.getRequestHeaders().get("DeviceClass")).isNotNull();
            assertThat(accessLog.getStatus()).isEqualTo(200);
            assertThat(accessLog.getResponseHeaders().get("SAMPLE-RESPONSE-HEADER")).isEqualTo("1234567");
            assertThat(accessLog.getResponseBody()).isEqualTo("Hello response");
            assertThat(accessLog.getRequestBody()).isEqualTo("Hello world");
            assertThat(accessLog.getResponseHeaders()).isNotNull();
            assertThat(accessLog.getElapsed()).isNotNull();
        };

        // filterChain.doFilter 메소드 호출 시 대신 실행되어야 하는 코드를 정의
        doAnswer((Answer<Void>) invocationOnMock -> {
            // Response body 에 테스트용 데이터 작성
            CachingResponseWrapper responseWrapper = (CachingResponseWrapper) invocationOnMock.getArguments()[1];
            OutputStream outputStream = responseWrapper.getOutputStream();
            outputStream.write("Hello response".getBytes());
            return null;
        })
                .when(filterChain)
                .doFilter(any(), any());

        // 테스트할 필터
        AccessLogFilter accessLogFilter = new AccessLogFilter(accessLogFilterConfigurer, accessLogger);

        // When
        accessLogFilter.doFilterInternal(httpServletRequest, httpServletResponse, filterChain);

        // Then
        // Expect no exception
    }

    /**
     * Content 를 로깅하지 않는 테스트
     */
    @Test
    public void test_no_content_logging() throws Exception {

        // Given
        MockHttpServletRequest httpServletRequest = givenMockHttpServletRequest();
        MockHttpServletResponse httpServletResponse = givenMockHttpServletResponse();

        accessLogFilterConfigurer = AccessLogFilterConfigurer
                .builder()
                .enableContentLogging(false)
                .build();

        accessLogger = accessLog -> {
            // AccessLog 객체 내부에 정의된 필드가 존재하는지 확인한다.
            assertThat(accessLog.getRequestAt()).isNotNull();
            assertThat(accessLog.getResponseAt()).isNotNull();
            assertThat(accessLog.getUserAgent()).isNotNull();
            assertThat(accessLog.getHostName()).isNotNull();
            assertThat(accessLog.getIp()).isNotNull();
            assertThat(accessLog.getUri()).isEqualTo("/api/v1/foo/bar");
            assertThat(accessLog.getQuery()).isEqualTo("?hello=world");
            assertThat(accessLog.getMethod()).isEqualToIgnoringCase("post");
            assertThat(accessLog.getRequestHeaders().get("SAMPLE-HEADER")).isEqualTo("12345");
            assertThat(accessLog.getRequestHeaders().get("DeviceClass")).isNotNull();
            assertThat(accessLog.getStatus()).isEqualTo(200);
            assertThat(accessLog.getResponseHeaders().get("SAMPLE-RESPONSE-HEADER")).isEqualTo("1234567");
            assertThat(accessLog.getResponseBody()).isNull();
            assertThat(accessLog.getRequestBody()).isNull();
            assertThat(accessLog.getResponseHeaders()).isNotNull();
            assertThat(accessLog.getElapsed()).isNotNull();
        };

        // 테스트할 필터
        AccessLogFilter accessLogFilter = new AccessLogFilter(accessLogFilterConfigurer, accessLogger);

        // When
        accessLogFilter.doFilterInternal(httpServletRequest, httpServletResponse, filterChain);

        // Then
        // Expect no exception
    }

    /**
     * Content 길이 제한 테스트
     */
    @Test
    public void test_content_length_limit() throws Exception {

        // Given
        MockHttpServletRequest httpServletRequest = givenMockHttpServletRequest();
        MockHttpServletResponse httpServletResponse = givenMockHttpServletResponse();

        accessLogFilterConfigurer = AccessLogFilterConfigurer
                .builder()
                .enableContentLogging(true)
                .maxContentLength(2)
                .build();

        accessLogger = accessLog -> {
            // AccessLog 객체 내부에 정의된 필드가 존재하는지 확인한다.
            assertThat(accessLog.getRequestAt()).isNotNull();
            assertThat(accessLog.getResponseAt()).isNotNull();
            assertThat(accessLog.getUserAgent()).isNotNull();
            assertThat(accessLog.getHostName()).isNotNull();
            assertThat(accessLog.getIp()).isNotNull();
            assertThat(accessLog.getUri()).isEqualTo("/api/v1/foo/bar");
            assertThat(accessLog.getQuery()).isEqualTo("?hello=world");
            assertThat(accessLog.getMethod()).isEqualToIgnoringCase("post");
            assertThat(accessLog.getRequestHeaders().get("SAMPLE-HEADER")).isEqualTo("12345");
            assertThat(accessLog.getRequestHeaders().get("DeviceClass")).isNotNull();
            assertThat(accessLog.getStatus()).isEqualTo(200);
            assertThat(accessLog.getResponseHeaders().get("SAMPLE-RESPONSE-HEADER")).isEqualTo("1234567");
            assertThat(accessLog.getResponseBody()).isEqualTo("TOO LONG CONTENTS");
            assertThat(accessLog.getRequestBody()).isEqualTo("TOO LONG CONTENTS");
            assertThat(accessLog.getResponseHeaders()).isNotNull();
            assertThat(accessLog.getElapsed()).isNotNull();
        };

        // filterChain.doFilter 메소드 호출 시 대신 실행되어야 하는 코드를 정의
        doAnswer((Answer<Void>) invocationOnMock -> {
            // Response body 에 테스트용 데이터 작성
            CachingResponseWrapper responseWrapper = (CachingResponseWrapper) invocationOnMock.getArguments()[1];
            OutputStream outputStream = responseWrapper.getOutputStream();
            outputStream.write("Hello response".getBytes());
            return null;
        })
                .when(filterChain)
                .doFilter(any(), any());

        // 테스트할 필터
        AccessLogFilter accessLogFilter = new AccessLogFilter(accessLogFilterConfigurer, accessLogger);

        // When
        accessLogFilter.doFilterInternal(httpServletRequest, httpServletResponse, filterChain);

        // Then
        // Expect no exception
    }

    /**
     * White list 된 URI 에 대해 access log 를 남기지 않는 것을 테스트
     */
    @Test
    public void test_white_listed() throws Exception {

        // Given
        MockHttpServletRequest httpServletRequest = givenMockHttpServletRequest();
        MockHttpServletResponse httpServletResponse = givenMockHttpServletResponse();

        accessLogFilterConfigurer = AccessLogFilterConfigurer
                .builder()
                .whiteList(Collections.singletonList("/api/v1/foo/bar"))
                .build();

        accessLogger = accessLog -> fail("Should not reach here");

        // 테스트할 필터
        AccessLogFilter accessLogFilter = new AccessLogFilter(accessLogFilterConfigurer, accessLogger);

        // When
        accessLogFilter.doFilterInternal(httpServletRequest, httpServletResponse, filterChain);

        // Then
    }

    private MockHttpServletRequest givenMockHttpServletRequest() {

        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.addHeader("SAMPLE-HEADER", "12345");
        mockHttpServletRequest.addHeader("User-Agent", "SampleUserAgent");
        mockHttpServletRequest.setContent("Hello world".getBytes());
        mockHttpServletRequest.setMethod("post");
        mockHttpServletRequest.setRequestURI("/api/v1/foo/bar");
        mockHttpServletRequest.setQueryString("?hello=world");
        return mockHttpServletRequest;
    }

    private MockHttpServletResponse givenMockHttpServletResponse() throws IOException {

        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();
        mockHttpServletResponse.setStatus(200);
        mockHttpServletResponse.setHeader("SAMPLE-RESPONSE-HEADER", "1234567");
        mockHttpServletResponse.getOutputStream()
                .write("Hello response".getBytes());
        return mockHttpServletResponse;
    }
}