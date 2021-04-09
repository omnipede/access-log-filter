package com.coinplug;

import com.coinplug.system.filter.accesslog.AccessLog;
import com.coinplug.system.filter.accesslog.AccessLogFilter;
import com.coinplug.system.filter.accesslog.AccessLogFilterConfigurer;
import com.coinplug.system.filter.accesslog.AccessLogger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Slf4j
@Configuration
class SampleConfig {

    /**
     * Access log 필터 등록
     */
    @Bean
    public FilterRegistrationBean<AccessLogFilter> accessLogFilter(AccessLogFilterConfigurer accessLogFilterConfigurer, AccessLogger accessLogger) {
        FilterRegistrationBean<AccessLogFilter> filterRegistrationBean = new FilterRegistrationBean<>();

        filterRegistrationBean.setFilter(
            new AccessLogFilter(accessLogFilterConfigurer, accessLogger)
        );

        return filterRegistrationBean;
    }

    /**
     * Access log 필터 설정 bean 생성
     */
    @Bean
    public AccessLogFilterConfigurer accessLogFilterConfigurer() {
        return AccessLogFilterConfigurer.builder()
                // 로그를 남기지 않을 uri 리스트
                .whiteList(
                    Arrays.asList(
                        "/api/v1/health",
                        "/favicon.ico"
                    )
                )
                // 최대 contents 길이
                .maxContentLength(10 * 1024 * 1024)
                // Body 로깅 활성화 여부
                .enableContentLogging(true)
                // 설정 생성
                .build();
    }

    /**
     * Sample access logger 구현체 작성
     * @return Access logger 구현체 빈
     */
    @Bean
    public AccessLogger accessLogger() {
        return new AccessLogger() {
            private final Logger logger = LoggerFactory.getLogger(AccessLogger.class);
            private ObjectMapper objectMapper = new ObjectMapper();

            @Override
            public void log(AccessLog accessLog) {
                // For example, if access log contains secret field
                if (accessLog.getRequestBody() != null && accessLog.getRequestBody().contains("password"))
                    accessLog.setRequestBody("Secret hidden value.");

                try {
                    String message = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(accessLog);
                    logger.info(message);
                } catch (JsonProcessingException e) {
                    logger.info(e.getMessage());
                }
            }
        };
    }
}
