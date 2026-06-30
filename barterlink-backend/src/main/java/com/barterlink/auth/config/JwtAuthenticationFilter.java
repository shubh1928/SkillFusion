package com.barterlink.auth.config;

import com.barterlink.auth.helpers.UserHelper;
import com.barterlink.auth.repositories.UserRepository;
import com.barterlink.auth.services.impl.JwtService;
import io.jsonwebtoken.*;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")){

            String token =  header.substring(7);



            try {

                if (!jwtService.isAccessToken(token)){
                    filterChain.doFilter(request, response);
                    return;
                }


                //Extracts payload
                Jws<Claims> parse = jwtService.parse(token);
                Claims payload = parse.getPayload();

                //Extract User ID
                String userId = payload.getSubject();
                UUID userUuid = UserHelper.parseUUID(userId);

                userRepository.findById(userUuid)
                              .ifPresent(user -> {


                //check for user enable or not
                 if (user.isEnabled()){
                     List<GrantedAuthority> authorities = user.getRoles() == null ? List.of() : user.getRoles().stream()
                             .map(role -> new SimpleGrantedAuthority(role.getName())).collect(Collectors.toList());

                     UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                             user.getEmail(),
                             null,
                             authorities
                     );

                     authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                     if (SecurityContextHolder.getContext().getAuthentication() == null)
                         SecurityContextHolder.getContext().setAuthentication(authentication);
                 }

                              });

            }catch (ExpiredJwtException e){
                request.setAttribute("error", "Token Expired!!!");
//                e.printStackTrace();
            } catch (Exception e){
                request.setAttribute("error", "Invalid Token!!!");
//                e.printStackTrace();
            }

        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        return request.getRequestURI().startsWith("/api/v1/auth/");
    }
}
