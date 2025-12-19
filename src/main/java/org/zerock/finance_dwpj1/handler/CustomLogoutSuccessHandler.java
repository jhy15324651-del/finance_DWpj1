package org.zerock.finance_dwpj1.handler;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {

    @Override
    public void onLogoutSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        String referer = request.getHeader("Referer");

        String redirectUrl = "/";

        if (referer != null &&
                !referer.contains("/user/login") &&
                !referer.contains("/user/logout") &&
                !referer.contains("/admin")) {   // ⭐ 이 줄 추가) {

            //
            if (referer.startsWith("http") &&
                    referer.contains(request.getServerName())) {

                redirectUrl = referer;
            }
        }

        response.sendRedirect(redirectUrl);
    }
}