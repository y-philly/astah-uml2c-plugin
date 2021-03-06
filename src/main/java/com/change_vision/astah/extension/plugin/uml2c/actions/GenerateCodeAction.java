package com.change_vision.astah.extension.plugin.uml2c.actions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.apache.velocity.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.change_vision.astah.extension.plugin.uml2c.Messages;
import com.change_vision.astah.extension.plugin.uml2c.cmodule.AbstractCModule;
import com.change_vision.astah.extension.plugin.uml2c.cmodule.CModuleFactory;
import com.change_vision.astah.extension.plugin.uml2c.codegenerator.CodeGenerator;
import com.change_vision.astah.extension.plugin.uml2c.astahutil.AstahUtil;
import com.change_vision.jude.api.inf.AstahAPI;
import com.change_vision.jude.api.inf.exception.InvalidUsingException;
import com.change_vision.jude.api.inf.exception.ProjectNotFoundException;
import com.change_vision.jude.api.inf.model.IClass;
import com.change_vision.jude.api.inf.model.IElement;
import com.change_vision.jude.api.inf.model.IModel;
import com.change_vision.jude.api.inf.project.ProjectAccessor;
import com.change_vision.jude.api.inf.ui.IPluginActionDelegate;
import com.change_vision.jude.api.inf.ui.IWindow;
import com.change_vision.jude.api.inf.view.IDiagramViewManager;
import com.change_vision.jude.api.inf.view.IViewManager;


public abstract class GenerateCodeAction implements IPluginActionDelegate {
    private static Logger logger = LoggerFactory.getLogger(CodeGenerator.class);

    public Object run(IWindow window) throws UnExpectedException {
        try {
            AstahAPI api = AstahAPI.getAstahAPI();
            ProjectAccessor projectAccessor = api.getProjectAccessor();
            @SuppressWarnings("unused")
            IModel iCurrentProject = projectAccessor.getProject();

            List<IClass> targetClasses = getTargetClasses(api);
            if (targetClasses.isEmpty()) {
                JOptionPane.showMessageDialog(window.getParent(),
                        Messages.getMessage("message.select_class"),
                        Messages.getMessage("title.select_class"),
                        JOptionPane.WARNING_MESSAGE);
                logger.warn("No target class was selected");
                return null;
            }

            logger.info("Project Path = {}", projectAccessor.getProjectPath());
            String outputDirPath = getOutputDirPath(window, projectAccessor);
            logger.info("Output Path = {}", outputDirPath);
            if (outputDirPath == null) return null; //canceled

            if (!ensureWritableOutputFolder(outputDirPath, window)) {
                return null;
            }

            for (IClass iClass: targetClasses) {
                AbstractCModule cModule = CModuleFactory.getCModule(iClass);
                logger.info("Module = {}", cModule.getClass().getSimpleName());
                generateCode(cModule, outputDirPath);
            }

            JOptionPane.showMessageDialog(window.getParent(),
                    Messages.getMessage("message.finish_generating"),
                    Messages.getMessage("title.finish_generating"),
                    JOptionPane.INFORMATION_MESSAGE);
            logger.info("Finished.");
        } catch (ProjectNotFoundException e) {
            logger.warn("Project Not Found.");
            JOptionPane.showMessageDialog(window.getParent(),
                    Messages.getMessage("message.project_not_found"),
                    Messages.getMessage("title.project_not_found"),
                    JOptionPane.WARNING_MESSAGE);
        } catch (ResourceNotFoundException e) {
            logger.warn("Template Not Found.");
            logger.warn(" - Template Search Path = {}", AstahUtil.getFullPathOfConfigDir());
            logger.warn(" - Exception = {}", e.getLocalizedMessage());
            JOptionPane.showMessageDialog(window.getParent(),
                    Messages.getMessage("message.not_found_template", AstahUtil.getFullPathOfConfigDir(), e.getLocalizedMessage()),
                    Messages.getMessage("title.not_found_template"),
                    JOptionPane.WARNING_MESSAGE);
        } catch (Throwable e) {
            logger.error("Unexpected Exception", e);
            JOptionPane.showMessageDialog(window.getParent(),
                    Messages.getMessage("message.unexpected_exception", e.getLocalizedMessage()),
                    Messages.getMessage("title.unexpected_exception"),
                    JOptionPane.ERROR_MESSAGE);
        }

        return null;
    }

    protected abstract void generateCode(AbstractCModule cModule, String outputDirPath)
        throws IOException, InterruptedException;

    private boolean ensureWritableOutputFolder(String outputDirPath, IWindow window) {
        File outputDir = new File(outputDirPath);
        if (!outputDir.exists()) {
            boolean wasSucceeded = outputDir.mkdirs();
            if (!wasSucceeded) {
                logger.warn("Failed to create a folder to output. path = {}", outputDir.getAbsolutePath());
                JOptionPane.showMessageDialog(window.getParent(),
                        Messages.getMessage("message.failed_mkdirs", outputDir.getAbsolutePath()),
                        Messages.getMessage("title.failed_mkdirs"),
                        JOptionPane.WARNING_MESSAGE);
                return false;
            }
            logger.info("Created a folder to output. path = {}", outputDir.getAbsolutePath());
        }
        if (!outputDir.canWrite()) {
            logger.warn("The output folder is not writable. path = {}", outputDir.getAbsolutePath());
            JOptionPane.showMessageDialog(window.getParent(),
                    Messages.getMessage("message.not_writable_output_folder", outputDir.getAbsolutePath()),
                    Messages.getMessage("title.not_writable_output_folder"),
                    JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    private List<IClass> getTargetClasses(AstahAPI api) throws InvalidUsingException {
        IElement[] iElements = getSelectedElements(api);
        List<IClass> targetClasses = new ArrayList<IClass>();
        for (IElement element : iElements) {
            if (element instanceof IClass) {
                targetClasses.add((IClass)element);
            }
        }
        return targetClasses;
    }

    private String getOutputDirPath(IWindow window, ProjectAccessor projectAccessor)
            throws ProjectNotFoundException {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle(Messages.getMessage("title.choose_output_dir"));

        File projectDir = new File(projectAccessor.getProjectPath()).getParentFile();
        if (projectDir != null && projectDir.exists() && projectDir.canWrite()) {
            fileChooser.setSelectedFile(projectDir);
        }

        int selected = fileChooser.showSaveDialog(window.getParent());
        if (selected != JFileChooser.CANCEL_OPTION) {
            return fileChooser.getSelectedFile().getAbsolutePath();
        } else {
            return null; //canceled
        }
    }

    private IElement[] getSelectedElements(AstahAPI api) throws InvalidUsingException {
        IViewManager iViewManager = api.getViewManager();
        IDiagramViewManager iDiagramViewManager = iViewManager.getDiagramViewManager();
        return iDiagramViewManager.getSelectedElements();
    }
}
