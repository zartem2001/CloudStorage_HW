package com.example.cloudStorage.interceptor;


import com.example.cloudStorage.exception.UnauthorizedException;
import com.example.cloudStorage.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthService authService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        if ("/login".equals(path)) {
            return true;
        }

        String authToken = request.getHeader("auth-token");
        if (authToken == null || authToken.isBlank()) {
            log.warn("Запрос без auth-token на {}", path);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Отсутствует токен авторизации");
            return false;
        }

        try {
            var user = authService.getUserByToken(authToken);
            request.setAttribute("user", user);
            log.debug("Пользователь {} авторизован для {}", user.getLogin(), path);
            return true;
        } catch (UnauthorizedException e) {
            log.warn("Недействительный токен для {}", path);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
            return false;
        }
    }
}
