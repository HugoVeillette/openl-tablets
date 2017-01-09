package org.openl.rules.webstudio.web.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.ui.WebStudio;

@ManagedBean
@RequestScoped
public class ModulePageBean {
    private WebStudio studio = WebStudioUtils.getWebStudio();

    /**
     * @return Map &lt;String, List&lt;String&gt;&gt; of the imports from
     *         Environment table
     */
    public Map<String, Set<String>> getTableSyntaxNodes() {
        Set<String> values;
        String key;
        String value;
        // Getting nodes from Model
        TableSyntaxNode[] nodes = studio.getModel().getTableSyntaxNodes();

        // Creating a list of environment tables. If tables more than 1
        List<TableSyntaxNode> envNodesTables = new LinkedList<TableSyntaxNode>();

        Map<String, Set<String>> ret = new HashMap<String, Set<String>>();

        // Filling the envNodesTables List by TableSyntaxNodes
        for (TableSyntaxNode node : nodes) {
            if ("xls.environment".equals(node.getType())) {
                envNodesTables.add(node);
            }
        }

        for (TableSyntaxNode node : envNodesTables) {
            for (int row = 1; row < node.getGridTable().getHeight(); row++) {
                key = node.getGridTable().getCell(0, row).getStringValue();
                value = node.getGridTable().getCell(1, row).getStringValue();

                if (ret.containsKey(key)) {
                    ret.get(key).add(value);
                } else {
                    values = new LinkedHashSet<String>();
                    values.add(value);
                    ret.put(key, values);
                }
            }
        }

        return ret;
    }

    /**
     * 
     * @return List of all imoirts
     */
    public List<String> getImports() {
        Set<String> imports = getTableSyntaxNodes().get("import");
        if (imports != null) {
            return new ArrayList<String>(imports);
        }
        return Collections.emptyList();
    }

    /**
     * 
     * @return List of all includes
     */
    public List<String> getIncludes() {
        Set<String> includes = getTableSyntaxNodes().get("include");

        if (includes != null) {
            return removeXLSExtention(includes);
        }

        return Collections.emptyList();
    }

    public List<String> getDependencies() {
        Set<String> dependencies = getTableSyntaxNodes().get("dependency");

        if (dependencies != null) {
            return new ArrayList<String>(dependencies);
        }
        return Collections.emptyList();
    }

    /**
     * Removes .xls into include or dependency file/module
     * 
     * @param lists
     * @return
     */
    private List<String> removeXLSExtention(Collection<String> lists) {
        String[] dependencyFiles;
        List<String> dependencyFilesList = new ArrayList<String>();

        for (String dependency : lists) {
            if (dependency != null) {
                dependencyFiles = dependency.split("/");
                dependencyFilesList.add(dependencyFiles[dependencyFiles.length - 1].split(".xls")[0]);
            }
        }

        return dependencyFilesList;
    }

}
