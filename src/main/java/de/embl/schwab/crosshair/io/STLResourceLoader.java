package de.embl.schwab.crosshair.io;

import customnode.CustomMesh;
import customnode.CustomTriangleMesh;
import ij.IJ;
import org.scijava.vecmath.Point3f;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;

/**
 * Class to load 3D models saved in STL file format from the resources directory
 * adapted from https://github.com/fiji/3D_Viewer/blob/master/src/main/java/customnode/STLLoader.java to use
 * input stream, and load from resource name
 */
public class STLResourceLoader {
    private HashMap<String, CustomMesh> meshes;
    private ArrayList<Point3f> vertices = new ArrayList();
    private String name = null;
    private final Point3f normal = new Point3f(0.0F, 0.0F, 0.0F);
    private int triangles;

    private STLResourceLoader() {
    }

    /**
     * Load the named STL 3D model from resources
     * @param name Name of the STL model e.g. "/arc.stl"
     * @return Map of STL model name to mesh
     */
    public static Map<String, CustomMesh> loadSTL(String name) {
        try {
            return STLResourceLoader.load(name);
        } catch (Exception var2) {
            var2.printStackTrace();
            return null;
        }
    }

    /**
     * Load the named STL 3D model from resources
     * @param name Name of the STL model e.g. "/arc.stl"
     * @return Map of STL model name to mesh
     * @throws IOException
     */
    public static Map<String, CustomMesh> load(String name) throws IOException {
        STLResourceLoader sl = new STLResourceLoader();

        try {
            sl.parse(name);
        } catch (RuntimeException var3) {
            IJ.log("error reading " + sl.name);
            throw var3;
        }

        return sl.meshes;
    }

    private void parse(String name) throws IOException {
        this.name = name;
        InputStream inputStream = getClass().getResourceAsStream(name);
        byte[] buffer = new byte[84];
        inputStream.read(buffer, 0, 84);
        this.triangles = (buffer[83] & 255) << 24 | (buffer[82] & 255) << 16 | (buffer[81] & 255) << 8 | buffer[80] & 255;
        inputStream.close();
        this.parseBinary();
    }

    private void parseBinary() {
        InputStream inputStream = getClass().getResourceAsStream(name);
        this.meshes = new HashMap();
        this.vertices = new ArrayList();

        try {
            int t;
            for (t = 0; t < 84; ++t) {
                inputStream.read();
            }

            for (t = 0; t < this.triangles; ++t) {
                byte[] tri = new byte[50];
                inputStream.read(tri);

                this.normal.x = this.leBytesToFloat(tri[0], tri[1], tri[2], tri[3]);
                this.normal.y = this.leBytesToFloat(tri[4], tri[5], tri[6], tri[7]);
                this.normal.z = this.leBytesToFloat(tri[8], tri[9], tri[10], tri[11]);

                int i;
                for (i = 0; i < 3; ++i) {
                    int j = i * 12 + 12;
                    float px = this.leBytesToFloat(tri[j], tri[j + 1], tri[j + 2], tri[j + 3]);
                    float py = this.leBytesToFloat(tri[j + 4], tri[j + 5], tri[j + 6], tri[j + 7]);
                    float pz = this.leBytesToFloat(tri[j + 8], tri[j + 9], tri[j + 10], tri[j + 11]);
                    Point3f p = new Point3f(px, py, pz);
                    this.vertices.add(p);
                }
            }

            inputStream.close();
        } catch (IOException var10) {
            var10.printStackTrace();

        }

        CustomMesh cm = this.createCustomMesh();
        this.meshes.put(this.name, cm);
    }

    private float leBytesToFloat(byte b0, byte b1, byte b2, byte b3) {
        return Float.intBitsToFloat((b3 & 255) << 24 | (b2 & 255) << 16 | (b1 & 255) << 8 | b0 & 255);
    }

    private CustomMesh createCustomMesh() {
        if (this.vertices.size() == 0) {
            return null;
        } else {
            CustomMesh cm = null;
            cm = new CustomTriangleMesh(this.vertices);
            return cm;
        }
    }
}
