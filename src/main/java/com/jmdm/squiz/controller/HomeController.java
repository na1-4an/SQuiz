package com.jmdm.squiz.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collection;
import java.util.Iterator;

@Controller
public class HomeController {
    // 기본 페이지 요청 메서드
    @GetMapping("/")
    public String homeP() {
        return "index";
    }

// 로그인 후 stateless 한 세션이 만들어져서 이름과 role을 땡겨 올 수 있음
//    @ResponseBody
//    public String mainP() {
//        String username = SecurityContextHolder.getContext().getAuthentication().getName();
//
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//
//        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
//        Iterator<? extends GrantedAuthority> iter = authorities.iterator();
//        GrantedAuthority auth = iter.next();
//        String role = auth.getAuthority();
//
//        return "Home Controller" + username + role; // => templates 폴더의 index.html을 찾아감
//    }
}
