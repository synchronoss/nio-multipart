package org.synchronoss.cloud.nio.multipart.io;


/**
 * A {@code Dismissable} is a destination of data that can be discarded.
 * The dismiss method is invoked to dismiss resources that the object is
 * holding (such as open files or streams).
 *
 */
public interface Dismissable {

    /**
     * Dismiss and releases any system resources associated with this object. This includes closing streams and deleting
     * any temporary files. If the resources are already discarded then invoking this method has no effect.
     *
     * @return <code>true</code> if and only if the file was created and it has been deleted successfully; <code>false</code> otherwise.
     */
    boolean dismiss();
}
