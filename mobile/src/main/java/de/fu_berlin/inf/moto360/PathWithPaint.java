package de.fu_berlin.inf.moto360;

import android.graphics.Paint;
import android.graphics.Path;

/**
 * Created by fjodor on 6/15/2015.
 */
public class PathWithPaint {
    private Path path;
    private Paint paint;

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public Paint getPaint() {
        return paint;
    }

    public void setPaint(Paint paint) {
        this.paint = paint;
    }
}
