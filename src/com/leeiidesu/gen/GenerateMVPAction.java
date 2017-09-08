package com.leeiidesu.gen;

import com.google.common.base.CaseFormat;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.actions.CreateFileFromTemplateDialog;
import com.intellij.ide.actions.JavaCreateTemplateInPackageAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidatorEx;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static org.jetbrains.dekaf.sql.Rewriters.replace;


/**
 * Created by dgg on 2017/6/19.
 */
public class GenerateMVPAction extends JavaCreateTemplateInPackageAction<PsiClass> {
    private static final String MODE_SINGLE_ACTIVITY = "single_activity";
    private static final String MODE_MULTI = "activity and fragment";
    private static final String MODE_FRAGMENT = "only fragment";

    private String packageName;
    private String projectPackage;

    protected GenerateMVPAction() {
        super("", IdeBundle.message("action.create.new.class.description"),
                PlatformIcons.CLASS_ICON, true);
    }

    @Nullable
    @Override
    protected PsiElement getNavigationElement(@NotNull PsiClass psiFile) {
        return psiFile.getLBrace();
    }

    protected final PsiClass doCreate(PsiDirectory dir, String className, String templateName)
            throws IncorrectOperationException {

        projectPackage = Utils.getProjectPackage(dir, dir.getProject());

        System.out.println("class name = " + className + " templateName = " + templateName);
        switch (templateName) {
            case MODE_SINGLE_ACTIVITY:
                PsiClass leeActivity2 = JavaDirectoryService.getInstance()
                        .createClass(dir, className + "Activity"
                                , "LeeActivitySingle");
                onProcessItemViewProvider(dir, className, leeActivity2);

                String qualifiedName3 = leeActivity2.getQualifiedName();
                this.packageName = qualifiedName3.replace("." + className + "Activity", "");
                createSameFile(dir, className, false);
                findResAndCreateLayoutXml(leeActivity2.getContainingFile().getParent(),
                        "activity_" + CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, className) + ".xml");
                return leeActivity2;
            case MODE_FRAGMENT:
                PsiClass leeFragment2 = JavaDirectoryService.getInstance()
                        .createClass(dir, className + "Fragment"
                                , "LeeFragmentSingle");
                onProcessItemViewProvider(dir, className, leeFragment2);

                String qualifiedName2 = leeFragment2.getQualifiedName();
                this.packageName = qualifiedName2.replace("." + className + "Fragment", "");
                createSameFile(dir, className, true);

                findResAndCreateLayoutXml(leeFragment2.getContainingFile().getParent(),
                        "fragment_" + CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, className) + ".xml");
                return leeFragment2;
            case MODE_MULTI:
                PsiClass leeFragment = JavaDirectoryService.getInstance()
                        .createClass(dir, className + "Fragment"
                                , "LeeFragment");
                PsiClass leeActivity = JavaDirectoryService.getInstance()
                        .createClass(dir, className + "Activity"
                                , "LeeActivity");

                onProcessItemViewProvider(dir, className, leeActivity, leeFragment);

                String qualifiedName = leeFragment.getQualifiedName();
                this.packageName = qualifiedName.replace("." + className + "Fragment", "");

                createSameFile(dir, className, false);
                findResAndCreateLayoutXml(leeActivity.getContainingFile().getParent(),
                        "activity_" + CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, className) + ".xml",
                        "fragment_" + CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, className) + ".xml");
                return leeActivity;
        }
        return null;
    }

    private void findResAndCreateLayoutXml(PsiDirectory containingClass, String... filename) {
        if (containingClass == null) return;
        PsiDirectory parent = containingClass.getParent();
        if (parent == null) return;
        PsiDirectory res = parent.findSubdirectory("res");

        if (res == null) {
            findResAndCreateLayoutXml(parent, filename);
        } else {
            PsiDirectory layout = res.findSubdirectory("layout");
            if (layout == null) return;

            for (String file :
                    filename) {
                PsiFile file1 = layout.findFile(file);
                if (file1 == null) {
                    PsiFile file2 = layout.createFile(file);
                    inputLayoutFile(file2);
                }
            }
        }
    }

    private void inputLayoutFile(PsiFile file) {
        final PsiDocumentManager manager = PsiDocumentManager.getInstance(file.getProject());
        final Document document = manager.getDocument(file);
        if (document == null) {
            return;
        }

        new WriteCommandAction.Simple(file.getProject()) {
            @Override
            protected void run() throws Throwable {
                manager.doPostponedOperationsAndUnblockDocument(document);

                StringBuilder builder = new StringBuilder();

                builder.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
                        .append("<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n")
                        .append("   xmlns:app=\"http://schemas.android.com/apk/res-auto\"\n")
                        .append("   android:layout_width=\"match_parent\"\n")
                        .append("   android:layout_height=\"match_parent\"\n")
                        .append("   android:orientation=\"vertical\">\n")
                        .append("\n");

                if (file.getName().startsWith("activity")) {
                    builder.append("")
                            .append("   <android.support.v7.widget.Toolbar\n")
                            .append("       android:id=\"@+id/toolbar\"\n")
                            .append("       android:layout_width=\"match_parent\"\n")
                            .append("       android:layout_height=\"?attr/actionBarSize\" />\n")
                            .append("\n")
                            .append("   <FrameLayout\n")
                            .append("       android:id=\"@+id/content\"\n")
                            .append("       android:layout_width=\"match_parent\"\n")
                            .append("       android:layout_height=\"match_parent\" />\n");


                }

                builder.append("</LinearLayout>");

                document.setText(builder.toString());
                CodeStyleManager.getInstance(file.getProject()).reformat(file);
            }
        }.execute();

    }


    private void createSameFile(PsiDirectory dir, String className, boolean isFragment) {
        //创建Presenter
        PsiClass leePresenter = JavaDirectoryService.getInstance()
                .createClass(dir, className + "Presenter",
                        "LeePresenter");
        //创建Contract
        PsiClass leeContract = JavaDirectoryService.getInstance()
                .createClass(dir, className + "Contract",
                        "LeeContract");
        //创建model
        PsiClass leeModel = JavaDirectoryService.getInstance()
                .createClass(dir, className + "Model",
                        "LeeModel");
        //创建Component
        PsiDirectory di;
        if ((di = dir.findSubdirectory("di")) == null)
            di = dir.createSubdirectory("di");
        PsiClass leeComponent;
        if (isFragment) {
            leeComponent = JavaDirectoryService.getInstance()
                    .createClass(di, className + "Component",
                            "LeeFragmentComponent");
        } else {
            leeComponent = JavaDirectoryService.getInstance()
                    .createClass(di, className + "Component",
                            "LeeComponent");
        }
        //创建module
        PsiClass leeModule = JavaDirectoryService.getInstance()
                .createClass(di, className + "Module",
                        "LeeModule");
        onProcessItemViewProvider(dir, className, leeComponent, leeContract, leeModel, leeModule, leePresenter);
    }

    private void onProcessItemViewProvider(PsiDirectory dir, String className, PsiClass... itemClass) {
        for (PsiClass item :
                itemClass) {
            processItemViewProvider(dir, className, item);
        }
    }

    private void processItemViewProvider(PsiDirectory dir, String className, PsiClass itemClass) {

        PsiFile file = itemClass.getContainingFile();
        final PsiDocumentManager manager = PsiDocumentManager.getInstance(itemClass.getProject());
        final Document document = manager.getDocument(file);
        if (document == null) {
            return;
        }

        new WriteCommandAction.Simple(itemClass.getProject()) {
            @Override
            protected void run() throws Throwable {
                manager.doPostponedOperationsAndUnblockDocument(document);

                String replace = document.getText()
                        .replace("CLASS_ORIGIN", className)
                        .replace("ALL_LOWER_NAME",
                                CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, className))
                        .replace("START_LOWER_NAME",
                                CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, className))
                        .replace("PACKAGE_ORIGIN",
                                packageName + "." + className);
                if (projectPackage != null) {
                    replace = replace.replace("PROJECT_PACKAGE",
                            projectPackage
                    );
                }
                document.setText(replace);
                CodeStyleManager.getInstance(itemClass.getProject()).reformat(itemClass);
            }
        }.execute();

    }


    @Override
    protected void buildDialog(Project project, PsiDirectory psiDirectory, CreateFileFromTemplateDialog.Builder builder) {


        builder.setTitle("Auto Generate")
                .addKind("单Activity", PlatformIcons.CLASS_ICON, MODE_SINGLE_ACTIVITY)
                .addKind("单Fragment", PlatformIcons.CLASS_ICON, MODE_FRAGMENT)
                .addKind("无逻辑Activity + Fragment", PlatformIcons.CLASS_ICON, MODE_MULTI);

        builder.setValidator(new InputValidatorEx() {
            @Nullable
            @Override
            public String getErrorText(String inputString) {
                if (inputString.length() > 0 &&
                        !PsiNameHelper.getInstance(project).isQualifiedName(inputString)) {
                    return "This is not a valid Java qualified name";
                }
                return null;
            }

            @Override
            public boolean checkInput(String inputString) {
                return true;
            }

            @Override
            public boolean canClose(String inputString) {
                return !StringUtil.isEmptyOrSpaces(inputString) &&
                        getErrorText(inputString) == null;
            }
        });


    }

    @Override
    protected String getActionName(PsiDirectory psiDirectory, String newName, String s1) {
        return IdeBundle.message("progress.creating.class",
                StringUtil.getQualifiedName(
                        JavaDirectoryService.getInstance().
                                getPackage(psiDirectory).
                                getQualifiedName(),
                        newName
                )
        );
    }

    @Override
    protected void postProcess(PsiClass createdElement, String templateName, Map<String, String> customProperties) {
        super.postProcess(createdElement, templateName, customProperties);

        moveCaretAfterNameIdentifier(createdElement);
    }
}
