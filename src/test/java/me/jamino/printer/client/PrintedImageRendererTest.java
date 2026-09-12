package me.jamino.printer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PrintedImageRendererTest {
    @ParameterizedTest
    @ValueSource(ints = {0xFFFFFF, 0xB02E26, 0x224466, 0x000000})
    void canvasFrontBackAndAllEdgesUseSelectedBackground(int background) {
        var vertices = new RecordingConsumer();
        PrintedImageRenderer.renderCanvas(new PoseStack().last(), vertices, 2, 1, background,
                (x, y) -> 0x00F000F0);

        assertEquals(40, vertices.colors.size());
        assertTrue(vertices.colors.stream().allMatch(color -> color == (0xFF000000 | background)));
        assertEquals(8, vertices.normals.stream().filter(n -> n.equals("0,0,-1")).count());
        assertEquals(8, vertices.normals.stream().filter(n -> n.equals("0,0,1")).count());
        assertEquals(8, vertices.normals.stream().filter(n -> n.equals("0,-1,0")).count());
        assertEquals(8, vertices.normals.stream().filter(n -> n.equals("0,1,0")).count());
        assertEquals(4, vertices.normals.stream().filter(n -> n.equals("-1,0,0")).count());
        assertEquals(4, vertices.normals.stream().filter(n -> n.equals("1,0,0")).count());
    }

    private static final class RecordingConsumer implements VertexConsumer {
        final List<Integer> colors = new ArrayList<>();
        final List<String> normals = new ArrayList<>();

        @Override public VertexConsumer addVertex(float x, float y, float z) { return this; }
        @Override public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            colors.add(alpha << 24 | red << 16 | green << 8 | blue);
            return this;
        }
        @Override public VertexConsumer setUv(float u, float v) { return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) {
            normals.add(Math.round(x) + "," + Math.round(y) + "," + Math.round(z));
            return this;
        }
    }
}
