/*
Copyright (C) 2001, 2009 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

package gov.nasa.worldwind.util;

import gov.nasa.worldwind.cache.*;
import gov.nasa.worldwind.geom.*;

import java.awt.*;

/**
 * Provides searching and interpolation of a grid of scalars.
 *
 * @author tag
 * @version $Id: ImageInterpolator.java 9226 2009-03-06 03:42:35Z tgaskins $
 */
public class ImageInterpolator
{
    private static final class Cell implements Cacheable
    {
        private final int m0, m1, n0, n1;
        private float minx, maxx, miny, maxy;
        private Cell[] children;

        public Cell(int m0, int m1, int n0, int n1)
        {
            this.m0 = m0;
            this.m1 = m1;
            this.n0 = n0;
            this.n1 = n1;
        }

        public long getSizeInBytes()
        {
            return 13 * 4;
        }

        public void build(int numLevels, int cellSize)
        {
            if (numLevels == 0)
                return;

            if (this.m1 - this.m0 <= cellSize && this.n1 - this.n0 <= cellSize)
                return;

            this.children = this.split(this.m0, this.m1, this.n0, this.n1);
            for (Cell t : this.children)
            {
                t.build(numLevels - 1, cellSize);
            }
        }

        public Cell[] split(int mm0, int mm1, int nn0, int nn1)
        {
            int mma = (mm1 - mm0 > 1 ? mm0 + (mm1 - mm0) / 2 : mm0 + 1);
            int nna = (nn1 - nn0 > 1 ? nn0 + (nn1 - nn0) / 2 : nn0 + 1);
            int mmb = mm1 - mm0 > 1 ? mma : mm0;
            int nnb = nn1 - nn0 > 1 ? nna : nn0;

            return new Cell[] {
                new Cell(mm0, mma, nn0, nna),
                new Cell(mmb, mm1, nn0, nna),
                new Cell(mm0, mma, nnb, nn1),
                new Cell(mmb, mm1, nnb, nn1)
            };
        }

        public void computeBounds(Dimension dim, float[] xs, float[] ys)
        {
            if (this.children != null)
            {
                for (Cell t : this.children)
                {
                    t.computeBounds(dim, xs, ys);
                }

                this.computeExtremes();
            }
            else
            {
                this.computeExtremes(dim, xs, ys);
            }
        }

        private void computeExtremes(Dimension dim, float[] xs, float[] ys)
        {
            this.minx = Float.MAX_VALUE;
            this.maxx = -Float.MAX_VALUE;
            this.miny = Float.MAX_VALUE;
            this.maxy = -Float.MAX_VALUE;

            for (int j = this.n0; j <= this.n1; j++)
            {
                for (int i = this.m0; i <= this.m1; i++)
                {
                    int k = j * dim.width + i;
                    float x = xs[k];
                    float y = ys[k];

                    if (x < this.minx)
                        this.minx = x;
                    if (x > this.maxx)
                        this.maxx = x;

                    if (y < this.miny)
                        this.miny = y;
                    if (y > this.maxy)
                        this.maxy = y;
                }
            }
        }

        private void computeExtremes()
        {
            this.minx = Float.MAX_VALUE;
            this.maxx = -Float.MAX_VALUE;
            this.miny = Float.MAX_VALUE;
            this.maxy = -Float.MAX_VALUE;

            if (this.children == null)
                return;

            for (Cell t : children)
            {
                if (t.minx < this.minx)
                    this.minx = t.minx;
                if (t.maxx > this.maxx)
                    this.maxx = t.maxx;

                if (t.miny < this.miny)
                    this.miny = t.miny;
                if (t.maxy > this.maxy)
                    this.maxy = t.maxy;
            }
        }
    }

    public static class ContainingCell
    {
        public final int m0, m1, n0, n1;
        public final float minx, maxx, miny, maxy;
        public final double[] uv;
        public final int[] fieldIndices;

        private ContainingCell(Cell cell, double uv[], int[] fieldIndices)
        {
            this.uv = uv;

            this.m0 = cell.m0;
            this.m1 = cell.m1;
            this.n0 = cell.n0;
            this.n1 = cell.n1;

            this.minx = cell.minx;
            this.maxx = cell.maxx;
            this.miny = cell.miny;
            this.maxy = cell.maxy;

            this.fieldIndices = fieldIndices;
        }
    }

    private final Dimension gridSize;
    private final Cell root;
    private final float[] xs;
    private final float[] ys;
    private final int cellSize;
    private final BasicMemoryCache kidCache = new BasicMemoryCache(750000L, 1000000L);

    public ImageInterpolator(Dimension gridSize, float[] xs, float[] ys, int depth, int cellSize)
    {
        if (gridSize == null)
        {
            String message = Logging.getMessage("nullValue.DimensionIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (gridSize.width < 2 || gridSize.height < 2)
        {
            String message = Logging.getMessage("generic.DimensionsTooSmall");
            Logging.logger().log(java.util.logging.Level.SEVERE, message,
                new Object[] {gridSize.width, gridSize.height});
            throw new IllegalStateException(message);
        }

        if (xs == null || ys == null || xs.length < 4 || ys.length < 4)
        {
            String message = Logging.getMessage("Grid.ArraysInvalid");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (depth < 0)
        {
            String message = Logging.getMessage("Grid.DepthInvalid");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (cellSize < 1)
        {
            String message = Logging.getMessage("Grid.CellSizeInvalid");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        this.gridSize = gridSize;
        this.cellSize = cellSize;

        this.xs = xs;
        this.ys = ys;

        this.root = new Cell(0, this.gridSize.width - 1, 0, this.gridSize.height - 1);
        this.root.build(depth, this.cellSize);
        this.root.computeBounds(this.gridSize, this.xs, this.ys);
    }

    public ContainingCell findContainingCell(float x, float y)
    {
        return this.findContainingCell(this.root, x, y);
    }

    private ContainingCell findContainingCell(Cell cell, float x, float y)
    {
        if (x < cell.minx || x > cell.maxx || y < cell.miny || y > cell.maxy)
            return null;

        if (cell.m1 - cell.m0 <= this.cellSize && cell.n1 - cell.n0 <= this.cellSize)
            return this.checkContainment(x, y, cell);

        Cell[] kids = cell.children != null ? cell.children : (Cell[]) this.kidCache.getObject(cell);
        if (kids == null)
        {
            kids = cell.split(cell.m0, cell.m1, cell.n0, cell.n1);
            for (Cell child : kids)
            {
                child.computeExtremes(this.gridSize, this.xs, this.ys);
            }
            if (cell.children == null)
                this.kidCache.add(cell, kids, 4 * kids[0].getSizeInBytes());
        }

        for (Cell t : kids)
        {
            ContainingCell cellFound = this.findContainingCell(t, x, y);
            if (cellFound != null)
                return cellFound;
        }

        return null;
    }

    private ContainingCell checkContainment(float x, float y, Cell cell)
    {
        BarycentricQuadrilateral bq = makeBarycentricQuadrilateral(cell);

        double[] uv = bq.invertBilinear(new Vec4(x, y, 0));

        return uv != null ? new ContainingCell(cell, uv, getFieldIndices(cell)) : null;
    }

    private BarycentricQuadrilateral makeBarycentricQuadrilateral(Cell cell)
    {
        int i = index(cell.m0, cell.n0);
        int j = index(cell.m1, cell.n0);
        int k = index(cell.m1, cell.n1);
        int l = index(cell.m0, cell.n1);

        return new BarycentricQuadrilateral(
            new Vec4(this.xs[i], this.ys[i], 0),
            new Vec4(this.xs[j], this.ys[j], 0),
            new Vec4(this.xs[k], this.ys[k], 0),
            new Vec4(this.xs[l], this.ys[l], 0));
    }

    private int[] getFieldIndices(Cell cell)
    {
        return new int[] {
            index(cell.m0, cell.n0),
            index(cell.m1, cell.n0),
            index(cell.m1, cell.n1),
            index(cell.m0, cell.n1)
        };
    }

    public int index(int i, int j)
    {
        return j * this.gridSize.width + i;
    }
//
//    public static void main(String[] args)
//    {
//        int width = 2048;
//        int height = 2048;
//
//        float[] xs = new float[width * height];
//        float[] ys = new float[xs.length];
//
//        float dx = 360f / width;
//        float dy = 180f / height;
//
//        for (int j = 0; j < height; j++)
//        {
//            for (int i = 0; i < width; i++)
//            {
//                xs[j * width + i] = i * dx;
//                ys[j * width + i] = j * dy;
//            }
//        }
//
//        Runtime.getRuntime().gc();
//        long mem = Runtime.getRuntime().totalMemory();
//        ImageInterpolator grid = new ImageInterpolator(new Dimension(width, height), xs, ys, 10, 1);
//        Runtime.getRuntime().gc();
//        System.out.println(Runtime.getRuntime().totalMemory() - mem);
//        ContainingCell cell = null;
//
//        long start = System.currentTimeMillis();
//        for (int i = 0; i < width * height; i++)
//        {
//            cell = grid.findContainingCell(50, 30);
//        }
//        System.out.println(System.currentTimeMillis() - start + " ms");
//        if (cell != null)
//            System.out.printf("[%d, %d] [%d, %d] ---- (%f, %f) (%f, %f)\n", cell.m0, cell.m1, cell.n0, cell.n1,
//                cell.minx, cell.maxx, cell.miny, cell.maxy);
//    }
}
