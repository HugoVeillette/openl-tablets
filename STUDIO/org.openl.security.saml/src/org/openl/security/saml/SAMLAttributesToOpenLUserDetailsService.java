package org.openl.security.saml;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.security.Privilege;
import org.openl.rules.security.SimplePrivilege;
import org.openl.rules.security.SimpleUser;
import org.openl.util.StringUtils;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.saml.userdetails.SAMLUserDetailsService;

public class SAMLAttributesToOpenLUserDetailsService implements SAMLUserDetailsService {
    private final String usernameAttribute;
    private final String firstNameAttribute;
    private final String lastNameAttribute;
    private final String groupsAttribute;
    /**
     * Must map to {@link org.openl.rules.security.Privilege}
     */
    private final GrantedAuthoritiesMapper authoritiesMapper;

    public SAMLAttributesToOpenLUserDetailsService(String usernameAttribute,
            String firstNameAttribute,
            String lastNameAttribute,
            String groupsAttribute,
            GrantedAuthoritiesMapper authoritiesMapper) {
        this.usernameAttribute = usernameAttribute;
        this.firstNameAttribute = firstNameAttribute;
        this.lastNameAttribute = lastNameAttribute;
        this.groupsAttribute = groupsAttribute;
        this.authoritiesMapper = authoritiesMapper;
    }

    @Override
    public Object loadUserBySAML(SAMLCredential credential) throws UsernameNotFoundException {
        final List<Privilege> grantedAuthorities = new ArrayList<Privilege>();
        String username = credential.getNameID().getValue();
        String firstName = null;
        String lastName = null;

        if (StringUtils.isNotBlank(usernameAttribute)) {
            username = credential.getAttributeAsString(usernameAttribute);
        }

        if (StringUtils.isNotBlank(firstNameAttribute)) {
            firstName = credential.getAttributeAsString(firstNameAttribute);
        }

        if (StringUtils.isNotBlank(lastNameAttribute)) {
            lastName = credential.getAttributeAsString(lastNameAttribute);
        }

        if (StringUtils.isNotBlank(groupsAttribute)) {
            String[] names = credential.getAttributeAsStringArray(groupsAttribute);
            if (names != null) {
                for (final String name : names) {
                    grantedAuthorities.add(new SimplePrivilege(name, name));
                }
            }
        }

        @SuppressWarnings("unchecked")
        List<Privilege> privileges = (List<Privilege>) authoritiesMapper.mapAuthorities(grantedAuthorities);
        return new SimpleUser(firstName, lastName, username, null, privileges);
    }
}
