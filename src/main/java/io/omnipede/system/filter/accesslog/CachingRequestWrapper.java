package io.omnipede.system.filter.accesslog;

import lombok.Getter;
import org.apache.commons.io.IOUtils;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

class CachingRequestWrapper extends HttpServletRequestWrapper {

    @Getter
    private byte[] contentsAsByteArray;

    public CachingRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        InputStream is = super.getInputStream();
        contentsAsByteArray = IOUtils.toByteArray(is);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        final ByteArrayInputStream bis = new ByteArrayInputStream(contentsAsByteArray);
        return new ContentCachingWrapperInputStream(bis);
    }

    private static class ContentCachingWrapperInputStream extends ServletInputStream {

        private InputStream is;

        public ContentCachingWrapperInputStream(InputStream bis) {
            is = bis;
        }

        @Override
        public boolean isFinished() {
            return true;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {

        }

        @Override
        public int read() throws IOException {
            return is.read();
        }

        @Override
        public int read(byte[] b) throws IOException {
            return is.read(b);
        }
    }
}
