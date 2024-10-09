package com.example.backend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String LOG_FILE_PATH = "src/main/resources/static/ipLog.txt";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String ipAddress = extractIpAddress(request);
        String requestUri = request.getRequestURI();
        String method = request.getMethod();
        String userAgent = request.getHeader("User-Agent");

        logger.info("Incoming request - IP: {}, URI: {}, Method: {}, User-Agent: {}",
                ipAddress, requestUri, method, userAgent);
        String logEntry = String.format("Timestamp: %s - Incoming request - IP: %s,User-Agent: %s%n",
                new Date(), ipAddress, userAgent);
        if (requestUri.equals("/")) {
            writeLogToFile(logEntry);
        }

        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Request completed - IP: {}, URI: {}, Duration: {}ms, Status: {}",
                    ipAddress, requestUri, duration, response.getStatus());
        }
    }

    private String extractIpAddress(HttpServletRequest request) {
        String[] headers = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // If the IP contains multiple addresses, take the first one
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    @Async
    public void writeLogToFile(String logEntry) {
        try {
            // Ensure the directory exists
            Path logFilePath = Paths.get(LOG_FILE_PATH);
            Files.createDirectories(logFilePath.getParent());

            // Open the file in append mode
            try (FileWriter fileWriter = new FileWriter(LOG_FILE_PATH, true);
                    PrintWriter printWriter = new PrintWriter(fileWriter)) {
                printWriter.print(logEntry);
            }
        } catch (IOException e) {
            logger.error("Failed to write log entry to file {}", e.getMessage());
        }
    }
}