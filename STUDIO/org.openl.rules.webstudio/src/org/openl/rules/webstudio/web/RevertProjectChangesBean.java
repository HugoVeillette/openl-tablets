package org.openl.rules.webstudio.web;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.ui.Message;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.web.diff.UploadExcelDiffController;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.source.SourceHistoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.PropertyResolver;

/**
 * @author Andrei Astrouski
 */
@ManagedBean
@RequestScoped
public class RevertProjectChangesBean {

    private final Logger log = LoggerFactory.getLogger(RevertProjectChangesBean.class);

    @ManagedProperty("#{environment}")
    private PropertyResolver propertyResolver;

    public RevertProjectChangesBean() {
    }

    public List<ProjectHistoryItem> getHistory() {
        ProjectModel model = WebStudioUtils.getProjectModel();
        String[] sourceNames = getSources();
        List<File> historyListFiles = model.getHistoryManager().get(sourceNames);

        String dateModifiedPattern = propertyResolver.getProperty("data.format.date") + " 'at' hh:mm:ss a";
        Map<String, List<ProjectHistoryItem>> sourceNameHistoryMap = historyListFiles.stream().map(f -> {
            String modifiedOnStr = new SimpleDateFormat(dateModifiedPattern).format(new Date(f.lastModified()));
            return new ProjectHistoryItem(f.lastModified(), modifiedOnStr, f.getName());
        }).collect(Collectors.groupingBy(ProjectHistoryItem::getSourceName));

        List<ProjectHistoryItem> history = new ArrayList<>();
        for (List<ProjectHistoryItem> files : sourceNameHistoryMap.values()) {
            //mark as current
            files.stream().max(Comparator.comparingLong(ProjectHistoryItem::getVersion)).ifPresent(f -> f.setCurrent(true));
            //mark as initial
            files.stream().min(Comparator.comparingLong(ProjectHistoryItem::getVersion)).ifPresent(f -> f.setDisabled(true));
            history.addAll(files);
        }

        Collections.sort(history, Comparator.comparingLong(ProjectHistoryItem::getVersion).reversed());
        return history;
    }

    public String[] getSources() {
        ProjectModel model = WebStudioUtils.getProjectModel();
        return model.getModuleSourceNames();
    }

    public String restore() throws Exception {
        String versionToRestoreParam = FacesUtils.getRequestParameter("toRestore");
        long versionToRestore = Long.parseLong(versionToRestoreParam);

        ProjectModel model = WebStudioUtils.getProjectModel();
        if (model != null) {
            model.getHistoryManager().restore(versionToRestore);
        }
        return null;
    }

    public String compare() {
        try {
            long[] versionsToCompare = getVersionsToCompare();

            ProjectModel model = WebStudioUtils.getProjectModel();
            SourceHistoryManager<File> historyManager = model.getHistoryManager();
            List<File> sources = historyManager.get(versionsToCompare);
            if (sources.size() == 2) {
                File file1ToCompare = sources.get(0);
                File file2ToCompare = sources.get(1);
                String file1Name = file1ToCompare.getName();
                String file2Name = file2ToCompare.getName();

                if (!file2Name.equals(file1Name)) {
                    // Try to get a previous version
                    file1ToCompare = historyManager.getPrev(file2ToCompare.lastModified());
                    if (file1ToCompare == null) {
                        // Get initial source
                        sources = historyManager.get(file2Name);
                        file1ToCompare = sources.get(0);
                    }
                }

                UploadExcelDiffController diffController = (UploadExcelDiffController) FacesUtils
                    .getBackingBean("uploadExcelDiffController");
                diffController.compare(Arrays.asList(file1ToCompare, file2ToCompare));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new Message("Error when comparing projects");
        }

        return null;
    }

    private long[] getVersionsToCompare() {
        String versionsToCompareParam = FacesUtils.getRequestParameter("toCompare");
        String[] versionsToCompareStr = versionsToCompareParam.split(",");

        long[] versionsToCompare = new long[versionsToCompareStr.length];
        for (int i = 0; i < versionsToCompareStr.length; i++) {
            versionsToCompare[i] = Long.parseLong(versionsToCompareStr[i]);
        }

        return versionsToCompare;
    }

    public void setPropertyResolver(PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;
    }
}
