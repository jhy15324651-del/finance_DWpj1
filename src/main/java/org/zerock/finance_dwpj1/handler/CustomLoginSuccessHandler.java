package org.zerock.finance_dwpj1.handler;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;



@Component
public class CustomLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final RequestCache requestCache = new HttpSessionRequestCache();

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        HttpSession session = request.getSession(false);

        // SavedRequest (권한 때문에 튕겨온 경우)
        SavedRequest savedRequest = requestCache.getRequest(request, response);
        if (savedRequest != null) {
            response.sendRedirect(savedRequest.getRedirectUrl());
            return;
        }

        // prevPage (로그인 버튼으로 들어온 경우)
        if (session != null) {
            String prevPage = (String) session.getAttribute("prevPage");
            if (prevPage != null) {
                session.removeAttribute("prevPage");

                // 외부로 리다이렉트 방지
                if (prevPage.startsWith("http") && !prevPage.contains(request.getServerName())) {
                    prevPage = "/";
                }

                response.sendRedirect(prevPage);
                return;
            }
        }

        // 3) fallback
        response.sendRedirect("/");
    }
}