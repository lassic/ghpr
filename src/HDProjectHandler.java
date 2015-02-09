import com.intellij.find.FindManager;
import com.intellij.find.findUsages.FindUsagesManager;
import com.intellij.find.impl.FindManagerImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiTreeChangeAdapter;
import com.intellij.psi.PsiTreeChangeEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by oriargov on 2/9/15.
 */
public class HDProjectHandler extends AbstractProjectComponent {

    private static final Key<HDProjectHandler> KEY = Key.create(HDProjectHandler.class.getName());
    public static final Logger LOGGER = Logger.getInstance(HDProjectHandler.class);

    private final FindUsagesManager findUsagesManager;
    private final PsiManager psiManager;

    private final Set<VirtualFile> filesToScan = new HashSet<VirtualFile>();

    private final AtomicInteger startupScanAttemptsLeft = new AtomicInteger(5);

    public PsiTreeChangeAdapter listener;

    protected HDProjectHandler(Project project, PsiManager psiManager) {
        super(project);
        this.findUsagesManager =
                ((FindManagerImpl) FindManager.getInstance(project)).getFindUsagesManager();
        this.psiManager = psiManager;

        project.putUserData(KEY, this);
    }

    public static HDProjectHandler get(Project project) {
        return project.getUserData(KEY);
    }

    @Override
    public void projectOpened() {
        final AtomicBoolean hasRun = new AtomicBoolean(false);

        ApplicationManager.getApplication().invokeLater(
                new Runnable() {
                    @Override
                    public void run() {
                        if (myProject.isInitialized()) {
                            hasRun.set(true);
                            fetchPullRequestInfo();

                            psiManager.addPsiTreeChangeListener(listener = new MyPsiTreeChangeAdapter());
                        }
                    }
                }, new Condition() {
                    @Override
                    public boolean value(Object o) {
                        return hasRun.get();
                    }
                }
        );
    }

    private void fetchPullRequestInfo() {

    }

    @Override
    public void projectClosed() {
        if (listener != null) psiManager.removePsiTreeChangeListener(listener);
    }

    private class MyPsiTreeChangeAdapter extends PsiTreeChangeAdapter {
        @Override
        public void childAdded(@NotNull PsiTreeChangeEvent event) {
            maybeInvalidate(event);
        }

        @Override
        public void childMoved(@NotNull PsiTreeChangeEvent event) {
            maybeInvalidate(event);
        }

        @Override
        public void childRemoved(@NotNull PsiTreeChangeEvent event) {
            maybeInvalidate(event);
        }

        @Override
        public void childrenChanged(@NotNull PsiTreeChangeEvent event) {
            maybeInvalidate(event);
        }

        @Override
        public void childReplaced(@NotNull PsiTreeChangeEvent event) {
            maybeInvalidate(event);
        }

        @Override
        public void propertyChanged(@NotNull PsiTreeChangeEvent event) {
            maybeInvalidate(event);
        }

        private void maybeInvalidate(PsiTreeChangeEvent event) {
            PsiFile file = event.getFile();
            if (file == null) {
                return;
            }

            VirtualFile virtualFile = file.getVirtualFile();
            synchronized (filesToScan) {
                filesToScan.add(virtualFile);
            }
        }
    }
}

