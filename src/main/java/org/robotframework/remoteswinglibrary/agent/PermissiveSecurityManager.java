package org.robotframework.remoteswinglibrary.agent;

import java.io.FileDescriptor;
import java.net.InetAddress;
import java.security.Permission;

/**
 * Created by wojtek on 18/09/15.
 */
public class PermissiveSecurityManager extends SecurityManager {
    public void checkAccept(String host, int port) {
        return;
    }

    public void checkAccess(Thread t) {
        return;
    }

    public void checkAccess(ThreadGroup g) {
        return;
    }

    public void checkAwtEventQueueAccess() {
        return;
    }

    public void checkConnect(String host, int port) {
        return;
    }

    public void checkConnect(String host, int port, Object context) {
        return;
    }

    public void checkCreateClassLoader() {
        return;
    }

    public void checkDelete(String file) {
        return;
    }

    public void checkExec(String cmd) {
        return;
    }

    public void checkExit(int status) {
        return;
    }

    public void checkLink(String lib) {
        return;
    }

    public void checkListen(int port) {
        return;
    }

    public void checkMemberAccess(Class<?> clazz, int which) {
        return;
    }

    public void checkMulticast(InetAddress maddr) {
        return;
    }

    public void checkMulticast(InetAddress maddr, byte ttl) {
        return;
    }

    public void checkPackageAccess(String pkg) {
        return;
    }

    public void checkPackageDefinition(String pkg) {
        return;
    }

    public void checkPermission(Permission perm) {
        return;
    }

    public void checkPermission(Permission perm, Object context) {
        return;
    }

    public void checkPrintJobAccess() {
        return;
    }

    public void checkPropertiesAccess() {
        return;
    }

    public void checkPropertyAccess(String key) {
        return;
    }

    public void checkRead(FileDescriptor fd) {
        return;
    }

    public void checkRead(String file) {
        return;
    }

    public void checkRead(String file, Object context) {
        return;
    }

    public void checkSecurityAccess(String target) {
        return;
    }

    public void checkSetFactory() {
        return;
    }

    public void checkSystemClipboardAccess() {
        return;
    }

    public boolean checkTopLevelWindow(Object window) {
        return true;
    }

    public void checkWrite(FileDescriptor fd) {
        return;
    }

    public void checkWrite(String file) {
        return;
    }

}
