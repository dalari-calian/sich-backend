package com.sich.user;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Principal usado pelo {@code JwtAuthenticationFilter}: decora o {@link UserDetails}
 * carregado pelo email (subject do token) com o {@code userId} extraído do claim do
 * próprio JWT, para que a resolução do usuário autenticado não dependa mais do email
 * (mutável) em nenhum ponto após a autenticação da requisição.
 */
public record AuthenticatedUser(Long userId, UserDetails delegate) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return delegate.getAuthorities();
    }

    @Override
    public String getPassword() {
        return delegate.getPassword();
    }

    @Override
    public String getUsername() {
        return delegate.getUsername();
    }

    @Override
    public boolean isEnabled() {
        return delegate.isEnabled();
    }

    @Override
    public boolean isAccountNonExpired() {
        return delegate.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return delegate.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return delegate.isCredentialsNonExpired();
    }
}
