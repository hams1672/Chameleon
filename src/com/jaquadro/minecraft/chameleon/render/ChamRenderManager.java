package com.jaquadro.minecraft.chameleon.render;

import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.locks.ReentrantLock;

public class ChamRenderManager
{
    private final ReentrantLock lock;

    private WorldVertexBufferUploader vboUploader = new WorldVertexBufferUploader();
    private Stack<ChamRender> freeRenderers = new Stack<ChamRender>();
    private Map<VertexBuffer, ChamRender> renderers = new HashMap<VertexBuffer, ChamRender>();

    public ChamRenderManager () {
        lock = new ReentrantLock();
    }

    public ChamRender getRenderer (VertexBuffer buffer) {
        lock.lock();

        try {
            if (renderers.containsKey(buffer))
                return renderers.get(buffer);

            if (freeRenderers.empty())
                freeRenderers.push(new ChamRender());

            ChamRender renderer = freeRenderers.pop();
            renderer.setVertexBuffer(buffer);

            renderers.put(buffer, renderer);

            return renderer;
        }
        finally {
            lock.unlock();
        }
    }

    public ChamRender startDrawing (VertexBuffer buffer) {
        return startDrawing(buffer, DefaultVertexFormats.BLOCK);
    }

    public ChamRender startDrawing (VertexBuffer buffer, VertexFormat format) {
        ChamRender renderer = getRenderer(buffer);
        try {
            buffer.begin(GL11.GL_QUADS, format);
        }
        catch (IllegalStateException e) { }

        return renderer;
    }

    public void finishDrawing (ChamRender renderer) {
        lock.lock();

        try {
            VertexBuffer worldRenderer = renderer.getVertexBuffer();
            if (worldRenderer == null)
                return;

            try {
                worldRenderer.finishDrawing();
                this.vboUploader.draw(worldRenderer);
            } catch (IllegalStateException e) {
            }

            renderer.setVertexBuffer(null);
            renderers.remove(worldRenderer);
            freeRenderers.push(renderer);
        }
        finally {
            lock.unlock();
        }
    }
}
