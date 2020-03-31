package com.tibco.ep.buildmavenplugin;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.FileLockInterruptionException;
import java.nio.channels.OverlappingFileLockException;

/**
 * Utility class to lock a file / directory
 *
 */
public class Locker {
    private RandomAccessFile m_raf;
    private FileChannel m_fc;
    private FileLock m_fl;
    
    /**
     * create a lock - if already locked block
     * @param f file or directory to lock
     */
    public Locker(File f) {

        // suport locking directories
        //
        File lockFile = f;
        if (f.isDirectory()) {
            lockFile = new File(f, ".lock");
            if (!lockFile.exists()) {
                try {
                    lockFile.createNewFile();
                } catch (IOException e1) {
                }
            }
        }
        
        boolean locked = false;
        while (!locked) {
            try {
                m_raf = new RandomAccessFile(lockFile, "rw");
                m_fc = m_raf.getChannel();
                m_fl = m_fc.lock();
                locked=true;
            } catch (FileLockInterruptionException e) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e1) {
                }
            } catch (OverlappingFileLockException e) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e1) {
                }
            } catch (IOException e) {
                System.out.println(e.toString());
                m_raf = null;
                m_fc = null;
                m_fl = null;
            }
        }
    }

    /**
     * release lock
     */
    public void release() {
        if (m_fl != null) {
            try {
                m_fl.release();
            }
            catch (IOException e) {
                System.out.println(e.toString());
            }
        }
    }
}
