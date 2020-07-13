package org.openl.rules.model.scaffolding;

import java.util.List;

public class DatatypeModel implements Model{

    private String parent;
    private String name;
    private List<FieldModel> fields;

    public DatatypeModel(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<FieldModel> getFields() {
        return fields;
    }

    public void setFields(List<FieldModel> fields) {
        this.fields = fields;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }
}
