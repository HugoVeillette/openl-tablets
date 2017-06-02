package org.openl.rules.webstudio.security;

import java.util.Collection;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;

public class OpenLAuthenticationProviderWrapper implements AuthenticationProvider {
    private final AuthenticationProvider delegate;
    private boolean groupsAreManagedInStudio = true;
    private final AuthenticationUserDetailsService<Authentication> authenticationUserDetailsService;

    public OpenLAuthenticationProviderWrapper(AuthenticationProvider delegate,
            AuthenticationUserDetailsService<Authentication> authenticationUserDetailsService) {
        this.delegate = delegate;
        this.authenticationUserDetailsService = authenticationUserDetailsService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!delegate.supports(authentication.getClass())) {
            return null;
        }

        Authentication delegatedAuth = delegate.authenticate(authentication);
        if (!groupsAreManagedInStudio) {
            return delegatedAuth;
        }

        if (delegatedAuth != null) {
            UserDetails userDetails = authenticationUserDetailsService.loadUserDetails(delegatedAuth);
            Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
            return new UsernamePasswordAuthenticationToken(delegatedAuth.getPrincipal(), delegatedAuth.getCredentials(), authorities);
        }
        return null;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return Authentication.class.isAssignableFrom(authentication);
    }

    public void setGroupsAreManagedInStudio(boolean groupsAreManagedInStudio) {
        this.groupsAreManagedInStudio = groupsAreManagedInStudio;
    }
}
