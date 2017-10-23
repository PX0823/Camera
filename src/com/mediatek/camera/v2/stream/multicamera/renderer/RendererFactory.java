package com.mediatek.camera.v2.stream.multicamera.renderer;

import android.app.Activity;

import junit.framework.Assert;

/**
 * render factory,create render.
 *
 */
public class RendererFactory {

    /**
     * create renderer, different type.
     *
     * @param activity
     *            activity
     * @param config
     *            render configuration
     * @return renderer renderer
     */
    public static Renderer createRenderer(Activity activity, RendererConfig config) {
        Renderer render = null;
        switch (config.getType()) {
        case RENDERER_PREVIEW:
            render = new PreviewRenderer(activity, config);
            break;
        case RENDERER_SNAPSHOT:
            render = new SnapshotRenderer(activity, config);
            break;
        case RENDERER_RECORDER:
            render = new RecorderRenderer(activity, config);
            break;
        case RENDERER_SCREEN:
            render = new ScreenRenderer(activity, config);
            break;
        default:
            Assert.assertTrue(false);
            break;
        }
        return render;
    }
}
