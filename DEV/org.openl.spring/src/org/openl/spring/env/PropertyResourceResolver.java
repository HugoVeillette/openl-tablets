package org.openl.spring.env;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.PropertyResolver;

/**
 * A helper which resolves ${...} placeholders with values from property
 * resources (system & environment properties, init-params, predefined
 * properties files...), then converts a comma delimited string to a list of
 * values and replaces and {appName} and {profile} tags with values from the
 * application context.
 * <p>
 * For example. If value is
 * <i>file:${user.home}/{appName}-{profile}.properties</i>, where {appName} is
 * "webstudio", and Spring active propfile is "prod, test", then result will be
 * the list of values: <i>["file:/home/openl/webstudio-prod.properties",
 * "file:/home/openl/webstudio-test.properties"]</i>
 * </p>
 * 
 * @author Yury Molchan
 */
public class PropertyResourceResolver {
    public static final String APP_NAME_TAG = "{appName}";
    public static final String PROFILE_TAG = "{profile}";
    private static final String SEPARATOR = "[\\s,]+"; // Delimiter is comma
    private String appName;
    private String[] profiles;
    private PropertyResolver resolver;

    public PropertyResourceResolver(PropertyResolver resolver, String appName, String... profiles) {
        this.resolver = resolver;
        this.appName = appName;
        this.profiles = profiles;
    }

    private String getAppName() {
        return appName;
    }

    private String[] getProfiles() {
        return profiles;
    }

    public List<String> resolvePlaceholders(String... values) {
        if (CollectionUtils.isEmpty(values)) {
            return Collections.emptyList();
        }
        ArrayList<String> result = new ArrayList<String>(32);
        for (String value : values) {
            String resolved = resolver.resolvePlaceholders(value);
            if (StringUtils.isBlank(resolved)) {
                log.debug("!       Empty: '{}'", new Object[]{value});
                continue;
            }
            String[] splitted = resolved.split(SEPARATOR);
            for (String s : splitted) {
                List<String> withTags = resolveTags(s);
                result.addAll(withTags);
            }
        }
        return result;
    }

    private List<String> resolveTags(String value) {
        String withApp = resolveAppName(value);
        List<String> withProfiles = resolveProfile(withApp);
        return withProfiles;
    }

    private String resolveAppName(String value) {
        if (value.contains(APP_NAME_TAG)) {
            String appName = getAppName();
            if (StringUtils.isBlank(appName)) {
                log.debug("- No app name: '{}'", new Object[]{value});
                value = "";
            } else {
                value = value.replace(APP_NAME_TAG, appName);
            }
        }
        return value;
    }

    private List<String> resolveProfile(String value) {
        if (StringUtils.isBlank(value)) {
            return Collections.emptyList();
        }
        if (value.contains(PROFILE_TAG)) {
            String[] profiles = getProfiles();
            if (CollectionUtils.isEmpty(profiles)) {
                log.debug("- No profiles: '{}'", new Object[]{value});
                return Collections.emptyList();
            } else {
                int size = profiles.length;
                ArrayList<String> result = new ArrayList<String>(size);
                for (String profile : profiles) {
                    result.add(value.replace(PROFILE_TAG, profile));
                }
                return result;
            }
        } else {
            return Collections.singletonList(value);
        }
    }

    private final Logger log = LoggerFactory.getLogger(PropertyResourceResolver.class);
}
