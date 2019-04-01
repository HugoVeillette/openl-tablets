package org.openl.rules.project.xml;

import java.io.InputStream;

import org.openl.rules.project.IRulesDeploySerializer;
import org.openl.rules.project.model.ObjectVersionConverter;
import org.openl.rules.project.model.RulesDeploy;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.security.NoTypePermission;

public class BaseRulesDeploySerializer<T> implements IRulesDeploySerializer {
    protected final XStream xstream;
    private final ObjectVersionConverter<RulesDeploy, T> rulesDeployVersionConverter;

    public BaseRulesDeploySerializer(ObjectVersionConverter<RulesDeploy, T> rulesDeployVersionConverter) {
        xstream = new XStream(new DomDriver()) {
            @Override
            public void aliasType(String name, Class type) {
                super.aliasType(name, type);
                allowTypeHierarchy(type);
            }
        };
        xstream.addPermission(NoTypePermission.NONE);
        xstream.allowTypeHierarchy(String.class);
        this.rulesDeployVersionConverter = rulesDeployVersionConverter;
    }

    @Override
    public String serialize(RulesDeploy source) {
        return xstream.toXML(rulesDeployVersionConverter.toOldVersion(source));
    }

    @Override
    public RulesDeploy deserialize(InputStream source) {
        @SuppressWarnings("unchecked")
        T oldVersion = (T) xstream.fromXML(source);
        return rulesDeployVersionConverter.fromOldVersion(oldVersion);
    }

    public XStream getXstream() {
        return xstream;
    }
}
