package com.netmontools.lookatnet.ui.local;

import android.os.Handler;
import android.os.Looper;

import com.netmontools.lookatnet.ui.local.model.Folder;
import com.netmontools.lookatnet.ui.local.model.FolderSizeDao;
import com.netmontools.lookatnet.ui.local.model.FolderSizeModel;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Вычисляет размеры файлов/папок в фоне и кэширует результат в Room.
 *
 * Кэш считается валидным, пока не изменилось mtime каталога
 * (а у вложенных каталогов - mtime самого каталога).
 *
 * LiveData обновляется по мере расчёта, поэтому UI получает размеры
 * постепенно, по одному элементу.
 */
public class FolderSizeRepository {

    private static volatile FolderSizeRepository instance;

    private final FolderSizeDao dao;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private final Map<String, Boolean> computing = new HashMap<>();

    private FolderSizeRepository(FolderSizeDao dao) {
        this.dao = dao;
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static FolderSizeRepository getInstance(FolderSizeDao dao) {
        if (instance == null) {
            synchronized (FolderSizeRepository.class) {
                if (instance == null) {
                    instance = new FolderSizeRepository(dao);
                }
            }
        }
        return instance;
    }

    public interface OnSizeComputed {
        void onSizeComputed();
    }

    /**
     * Вычислить размеры элементов каталога в фоне.
     * Для каждого элемента, размер которого изменился (mtime), пересчитывает
     * и обновляет соответствующий Folder в списке (по path) на main-потоке,
     * затем вызывает listener.onSizeComputed().
     */
    public void computeSizesIfStale(final File dir, final List<Folder> items,
                                    final OnSizeComputed listener) {
        if (dir == null || !dir.isDirectory()) return;
        synchronized (computing) {
            if (Boolean.TRUE.equals(computing.get(dir.getPath()))) return;
            computing.put(dir.getPath(), true);
        }
        executor.submit(new Runnable() {
            @Override
            public void run() {
                File[] files = dir.listFiles();
                if (files == null) return;
                for (final File f : files) {
                    if (!f.exists()) continue;
                    final String path = f.getPath();
                    long mt;
                    try {
                        mt = f.lastModified();
                    } catch (Exception e) {
                        continue;
                    }
                    if (isValid(path, mt)) {
                        // Кэш валиден - применяем размер к UI в фоне,
                        // без запроса к БД (уже прочитан).
                        final long cached = cachedSize(path, mt);
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                for (Folder folder : items) {
                                    if (path.equals(folder.getPath())) {
                                        if (folder.getSize() != cached) {
                                            folder.setSize(cached);
                                        }
                                        break;
                                    }
                                }
                                if (listener != null) listener.onSizeComputed();
                            }
                        });
                        continue;
                    }

                    long size;
                    if (f.isDirectory()) {
                        size = dirSize(f);
                    } else {
                        size = f.length();
                    }
                    dao.upsert(new FolderSizeModel(path, size, mt));
                    final long s = size;
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            for (Folder folder : items) {
                                if (path.equals(folder.getPath())) {
                                    folder.setSize(s);
                                    break;
                                }
                            }
                            if (listener != null) listener.onSizeComputed();
                        }
                    });
                }
                synchronized (computing) {
                    computing.put(dir.getPath(), false);
                }
            }
        });
    }

    /**
     * Вернуть кэшированный размер, если он валиден (mtime совпадает), иначе -1.
     */
    public long cachedSize(String path, long currentMtime) {
        FolderSizeModel m = dao.byPath(path);
        if (m != null && m.mtime == currentMtime) {
            return m.size;
        }
        return -1;
    }

    /**
     * Проверить, валиден ли кэш для данного пути.
     */
    public boolean isValid(String path, long currentMtime) {
        return cachedSize(path, currentMtime) >= 0;
    }

    /**
     * Рекурсивный размер каталога (включая вложенные).
     * Вызывается только в фоновом потоке.
     */
    private long dirSize(File dir) {
        long size = 0;
        File[] files = dir.listFiles();
        if (files == null) return 0;
        for (File f : files) {
            if (f.isDirectory()) {
                size += dirSize(f);
            } else if (f.isFile()) {
                size += f.length();
            }
        }
        return size;
    }

    /**
     * Закрыть executor (вызывать при завершении приложения).
     */
    public void shutdown() {
        executor.shutdown();
    }
}
