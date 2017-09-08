package com.leeiidesu.gen;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.EverythingGlobalScope;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;

import java.util.ArrayList;

/**
 * Created by dgg on 2017/7/28.
 */
public class Utils {
    private static final Logger log = Logger.getInstance(Utils.class);

    public static XmlFile findAndroidManifest(PsiElement element, Project project) {
        PsiFile[] psiFiles = resolveFile(element, project, "AndroidManifest.xml");

        return filterFile(psiFiles);
    }

    private static XmlFile filterFile(PsiFile[] psiFiles) {
        ArrayList<PsiFile> xmlFiles = new ArrayList<>();
        for (PsiFile file : psiFiles) {
            if (file.getModificationStamp() != 0) {
                boolean contains = file.getViewProvider().getVirtualFile().getPath().contains("/build/intermediates/manifest/");
                if (!contains)
                    xmlFiles.add(file);
//                System.out.println("provider = " + file.getViewProvider().toString());
            }
        }
//        System.out.println("length = " + xmlFiles.size());

        return (XmlFile) xmlFiles.get(0);
    }

    public static String getProjectPackage(PsiElement element, Project project) {
        XmlFile androidManifest = findAndroidManifest(element, project);


        XmlTag manifest = androidManifest.getRootTag();

        if (manifest != null) {
            return manifest.getAttributeValue("package");
        }
        return null;
    }

    private static PsiFile[] resolveFile(PsiElement element, Project project, String name) {

        // restricting the search to the current module - searching the whole project could return wrong layouts
        Module module = ModuleUtil.findModuleForPsiElement(element);
        PsiFile[] files = null;
        if (module != null) {
            // first omit libraries, it might cause issues like (#103)
            GlobalSearchScope moduleScope = module.getModuleContentScope();
            files = FilenameIndex.getFilesByName(project, name, moduleScope);
        }
        if (files == null || files.length <= 0) {
            // fallback to search through the whole project
            // useful when the project is not properly configured - when the resource directory is not configured
            files = FilenameIndex.getFilesByName(project, name, new EverythingGlobalScope(project));
            if (files.length <= 0) {
                return null; //no matching files
            }
        }

        // TODO - we have a problem here - we still can have multiple layouts (some coming from a dependency)
        // we need to resolve R class properly and find the proper layout for the R class
        for (PsiFile file : files) {
            log.info("Resolved file for name [" + name + "]: " + file.getVirtualFile());
        }


        return files;
    }
}
