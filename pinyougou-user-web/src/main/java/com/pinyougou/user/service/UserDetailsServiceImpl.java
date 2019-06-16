package com.pinyougou.user.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * 认证类
 * 认证有cas服务端完成，调用此类的时候已经认证完成，目的是构建角色列表
 */
public class UserDetailsServiceImpl implements UserDetailsService {
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		System.out.println("经过认证类："+username);
		// 构建角色集合
		List<GrantedAuthority> authorities = new ArrayList();
		authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
		return new User(username, "", authorities);
	}
}
