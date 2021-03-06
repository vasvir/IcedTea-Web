// Copyright (C) 2001-2003 Jon A. Maxwell (JAM)
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

package net.sourceforge.jnlp.cache;

import net.adoptopenjdk.icedteaweb.IcedTeaWebConstants;
import net.adoptopenjdk.icedteaweb.jnlp.version.Version;
import net.adoptopenjdk.icedteaweb.logging.Logger;
import net.adoptopenjdk.icedteaweb.logging.LoggerFactory;
import net.sourceforge.jnlp.util.PropertiesFile;

import java.io.File;
import java.net.URL;

import static net.adoptopenjdk.icedteaweb.i18n.Translator.R;

/**
 * Describes an entry in the cache.
 *
 * @author <a href="mailto:jmaxwell@users.sourceforge.net">Jon A. Maxwell (JAM)</a> - initial author
 * @version $Revision: 1.10 $
 */
public class CacheEntry {

    private final static Logger LOG = LoggerFactory.getLogger(CacheEntry.class);

    private static final String KEY_CONTENT_LENGTH = "content-length";
    private static final String KEY_LAST_MODIFIED = "last-modified";
    private static final String KEY_LAST_UPDATED = "last-updated";
    public static final String KEY_JNLP_PATH = "jnlp-path";

    /** the remote resource location */
    private final URL location;

    /** the requested version */
    private final Version version;

    /** info about the cached file */
    private final PropertiesFile properties;

    /**
     * Create a CacheEntry for the resources specified as a remote
     * URL.
     *
     * @param location the remote resource location
     * @param version the version of the resource
     */
    public CacheEntry(URL location, Version version) {
        this.location = location;
        this.version = version;
        
        File infoFile = CacheUtil.getCacheFile(location, version);
        infoFile = new File(infoFile.getPath() + CacheDirectory.INFO_SUFFIX); // replace with something that can't be clobbered

        properties = new PropertiesFile(infoFile, R("CAutoGen"));
    }

    /**
     * Returns the remote location this entry caches.
     * @return URL same as the one on which this entry was created
     */
    public URL getLocation() {
        return location;
    }

    /**
     * Returns the time in the local system clock that the file was
     * most recently checked for an update.
     * @return when the item was updated (in ms)
     */
    public long getLastUpdated() {
        return getLongKey(KEY_LAST_UPDATED);
    }

    /**
     * Sets the time in the local system clock that the file was
     * most recently checked for an update.
     * @param updatedTime the time (in ms) to be set as last updated time
     */
    public void setLastUpdated(long updatedTime) {
        setLongKey(KEY_LAST_UPDATED, updatedTime);
    }

    public long getRemoteContentLength() {
        return getLongKey(KEY_CONTENT_LENGTH);
    }

    public void setRemoteContentLength(long length) {
        setLongKey(KEY_CONTENT_LENGTH, length);
    }

    public void setJnlpPath(String jnlpPath) {
    	properties.setProperty(KEY_JNLP_PATH, jnlpPath);
    }

    public long getLastModified() {
        return getLongKey(KEY_LAST_MODIFIED);
    }

    public void setLastModified(long modifyTime) {
        setLongKey(KEY_LAST_MODIFIED, modifyTime);
    }

    private long getLongKey(String key) {
        try {
            return Long.parseLong(properties.getProperty(key));
        } catch (Exception ex) {
            return 0;
        }
    }

    private void setLongKey(String key, long value) {
        properties.setProperty(key, Long.toString(value));
    }

    /**
     * Returns whether there is a version of the URL contents in
     * the cache and it is up to date.
     *
     * @param lastModified - current time as get from server (in ms). Mostly value of "Last-Modified" http header'? 
     * @return whether the cache contains the version
     */
    public boolean isCurrent(long lastModified) {
        boolean cached = isCached();
        LOG.info("isCurrent:isCached {}", cached);

        if (!cached) {
            return false;
        }
        try {
            long cachedModified = Long.parseLong(properties.getProperty(KEY_LAST_MODIFIED));
            LOG.info("isCurrent:lastModified cache:{} actual:{}", cachedModified, lastModified);
            return lastModified > 0 && lastModified <= cachedModified;
        } catch (Exception ex){
            LOG.error(IcedTeaWebConstants.DEFAULT_ERROR_MESSAGE, ex);
            return cached;
        }
    }

    /**
     * Returns true if the cache has a local copy of the contents
     * of the URL matching the specified version string.
     *
     * @return true if the resource is in the cache
     */
    public boolean isCached() {
        File localFile = getCacheFile();
        if (!localFile.exists())
            return false;

        try {
            long cachedLength = localFile.length();
            long remoteLength = Long.parseLong(properties.getProperty(KEY_CONTENT_LENGTH, "-1"));

            LOG.info("isCached: remote:{} cached:{}", remoteLength, cachedLength);

            if (remoteLength >= 0 && cachedLength != remoteLength)
                return false;
            else
                return true;
        } catch (Exception ex) {
            LOG.error(IcedTeaWebConstants.DEFAULT_ERROR_MESSAGE, ex);

            return false; // should throw?
        }
    }

    /**
     * Seam for testing
     */
    File getCacheFile() {
        return CacheUtil.getCacheFile(location, version);
    }

    /**
     * Save the current information for the cache entry.
     *
     * @return True if successfully stored into file, false otherwise
     */
    protected boolean store() {
        if (properties.isHeldByCurrentThread()) {
            properties.store();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Mark this entry for deletion at shutdown.
     */
    public void markForDelete() { // once marked it should not be unmarked.
        properties.setProperty("delete", Boolean.toString(true));
    }

    /**
     * Lock cache item.
     */
    protected void lock() {
        properties.lock();
    }

    /**
     * Unlock cache item. Does not do anything if not holding the lock.
     */
    protected void unlock() {
        properties.unlock();
    }

    protected boolean tryLock() {
        return properties.tryLock();
    }

    protected boolean isHeldByCurrentThread() {
        return properties.isHeldByCurrentThread();
    }

}
