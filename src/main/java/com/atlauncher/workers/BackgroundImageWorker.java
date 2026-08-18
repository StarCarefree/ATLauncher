/*
 * ATLauncher - https://github.com/ATLauncher/ATLauncher
 * Copyright (C) 2013-2022 ATLauncher
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.atlauncher.workers;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingWorker;

import com.atlauncher.FileSystem;
import com.atlauncher.managers.LogManager;
import com.atlauncher.network.Download;
import com.atlauncher.network.DownloadException;

public class BackgroundImageWorker extends SwingWorker<ImageIcon, Object> {
    private final JLabel label;
    private final String url;
    private final int width;
    private final int height;

    public BackgroundImageWorker(JLabel label, String url, int width, int height) {
        this.label = label;
        this.url = url;
        this.width = width;
        this.height = height;
    }

    @Override
    protected ImageIcon doInBackground() throws Exception {
        Path path = cacheFile(this.url);

        // an empty or punctuation-only URL used to resolve to the cache folder itself.
        // Windows then throws AccessDeniedException when that directory is opened as a file
        if (path == null) {
            return null;
        }

        if (!Files.isRegularFile(path)) {
            try {
                Download.build().setUrl(this.url).ignoreFailures().downloadTo(path).downloadFile();
            } catch (DownloadException ignored) {
                // ignored
            }
        }

        if (!Files.isRegularFile(path)) {
            return null;
        }

        try (BufferedInputStream inputStream = new BufferedInputStream(Files.newInputStream(path))) {
            BufferedImage sourceImage = ImageIO.read(inputStream);

            if (sourceImage == null) {
                return null;
            }

            int newWidth = width;
            int newHeight = height;

            // Compute scales to maintain the aspect ratio
            if (sourceImage.getWidth() > sourceImage.getHeight()) {
                newHeight = (sourceImage.getHeight() * width) / sourceImage.getWidth();
            } else {
                newWidth = (sourceImage.getWidth() * height) / sourceImage.getHeight();
            }

            BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = scaledImage.createGraphics();
            g2d.drawImage(sourceImage, 0, 0, newWidth, newHeight, null);
            g2d.dispose();
            sourceImage.flush();

            return new ImageIcon(scaledImage);
        } catch (IOException e) {
            LogManager.debug("Failed to read cached image " + path);

            return null;
        }
    }

    /**
     * @return a file inside the image cache, or null when the URL would land on the cache
     *         directory itself
     */
    static Path cacheFile(String url) {
        if (url == null) {
            return null;
        }

        String key = url.replaceAll("[^A-Za-z0-9]", "");

        if (key.isEmpty()) {
            return null;
        }

        if (key.length() > 120) {
            key = key.substring(0, 88) + Integer.toHexString(url.hashCode());
        }

        Path path = FileSystem.REMOTE_IMAGE_CACHE.resolve(key);

        if (Files.isDirectory(path)) {
            return null;
        }

        return path;
    }

    @Override
    protected void done() {
        try {
            ImageIcon icon = get();

            if (icon != null) {
                label.setIcon(icon);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            LogManager.logStackTrace("Failed to load remote image", cause, false);
        } finally {
            label.setVisible(true);
        }
    }
}
