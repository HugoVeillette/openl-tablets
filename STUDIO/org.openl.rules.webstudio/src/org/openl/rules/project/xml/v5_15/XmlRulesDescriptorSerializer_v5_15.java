package org.openl.rules.project.xml.v5_15;

import org.openl.rules.project.model.v5_15.RulesDeploy_v5_15;
import org.openl.rules.project.model.v5_15.converter.RulesDeployVersionConverter;
import org.openl.rules.project.xml.BaseRulesDeploySerializer;

import com.thoughtworks.xstream.XStream;

public class XmlRulesDescriptorSerializer_v5_15 extends BaseRulesDeploySerializer<RulesDeploy_v5_15> {
    private static final String RULES_DEPLOY_DESCRIPTOR_TAG = "rules-deploy";
    private static final String MODULE_NAME = "module";
    private static final String LAZY_MODULES_FOR_COMPILATION = "lazy-modules-for-compilation";

    public XmlRulesDescriptorSerializer_v5_15() {
        super(new RulesDeployVersionConverter());
        xstream.ignoreUnknownElements();
        xstream.omitField(RulesDeploy_v5_15.class, "log");

        xstream.setMode(XStream.NO_REFERENCES);

        xstream.aliasType("publisher", RulesDeploy_v5_15.PublisherType.class);
        xstream.aliasType(RULES_DEPLOY_DESCRIPTOR_TAG, RulesDeploy_v5_15.class);
        xstream.aliasType(MODULE_NAME, RulesDeploy_v5_15.WildcardPattern.class);

        xstream.aliasField(LAZY_MODULES_FOR_COMPILATION, RulesDeploy_v5_15.class, "lazyModulesForCompilationPatterns");

        xstream.aliasField("name", RulesDeploy_v5_15.WildcardPattern.class, "value");
        xstream.useAttributeFor(RulesDeploy_v5_15.WildcardPattern.class, "value");
    }
}
