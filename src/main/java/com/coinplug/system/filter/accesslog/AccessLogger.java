package com.coinplug.system.filter.accesslog;

/**
 * 로그를 남길 때 사용하는 인터페이스
 */
public interface AccessLogger {

    void log(AccessLog accessLog);
}
