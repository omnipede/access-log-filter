package com.coinplug.system.filter.accesslog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Access log 필터 설정 클래스
 * @author 서현규
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccessLogFilterConfigurer {

    // 로그를 남기지 않을 URI 리스트
    private List<String> whiteList;

    // Maximum request, response contents length
    private int maxContentLength = 1024;

    // Request, response body 를 로그로 남길지 여부
    private boolean enableContentLogging = false;
}
