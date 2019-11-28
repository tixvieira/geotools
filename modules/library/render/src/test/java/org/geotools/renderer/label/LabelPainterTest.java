/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2014 - 2016 , Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.renderer.label;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.geotools.geometry.jts.LiteShape2;
import org.geotools.referencing.operation.transform.ProjectiveTransform;
import org.geotools.renderer.label.LabelCacheImpl.LabelRenderingMode;
import org.geotools.renderer.style.MarkStyle2D;
import org.geotools.renderer.style.Style2D;
import org.geotools.renderer.style.TextStyle2D;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.StyleFactoryImpl;
import org.geotools.styling.TextSymbolizer;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.mockito.Mockito;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

public class LabelPainterTest {

    private static GeometryFactory geometryFactory = new GeometryFactory();
    private static StyleFactory styleFactory = new StyleFactoryImpl();
    private Graphics2D graphics;
    private TextStyle2D style;
    private TextSymbolizer symbolizer;
    LiteShape2 shape;

    @Before
    public void setUp() throws TransformException, FactoryException {
        graphics = Mockito.mock(Graphics2D.class);
        Mockito.when(graphics.getFontRenderContext())
                .thenReturn(
                        new FontRenderContext(
                                new AffineTransform(),
                                RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT,
                                RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT));
        style = new TextStyle2D();
        style.setFont(new Font("Serif", Font.PLAIN, 10));
        shape =
                new LiteShape2(
                        geometryFactory.createPoint(new Coordinate(10, 10)),
                        ProjectiveTransform.create(new AffineTransform()),
                        null,
                        false);
        symbolizer = styleFactory.createTextSymbolizer();
    }

    @Test
    public void testEmptyLinesInLabel() {
        LabelPainter painter = new LabelPainter(graphics, LabelRenderingMode.STRING);
        LabelCacheItem labelItem =
                new LabelCacheItem("LAYERID", style, shape, "line1\n\nline2", symbolizer);
        labelItem.setAutoWrap(0);
        painter.setLabel(labelItem);
        assertEquals(3, painter.getLineCount());
    }

    @Test
    public void testEmptyLinesInLabelWithAutoWrap() {
        LabelPainter painter = new LabelPainter(graphics, LabelRenderingMode.STRING);
        LabelCacheItem labelItem =
                new LabelCacheItem("LAYERID", style, shape, "line1\n\nline2", symbolizer);
        labelItem.setAutoWrap(100);
        painter.setLabel(labelItem);
        assertEquals(3, painter.getLineCount());
    }

    @Test
    public void testOnlyNewlines() {
        LabelPainter painter = new LabelPainter(graphics, LabelRenderingMode.STRING);
        LabelCacheItem labelItem = new LabelCacheItem("LAYERID", style, shape, "\n\n", symbolizer);
        labelItem.setAutoWrap(100);
        painter.setLabel(labelItem);
        // emtpy label
        assertEquals(0, painter.getLineCount());
    }

    @Test
    public void testGetLastLineHeightOnlyNewLines() {
        LabelPainter painter = new LabelPainter(graphics, LabelRenderingMode.STRING);
        LabelCacheItem labelItem = new LabelCacheItem("LAYERID", style, shape, "\n\n", symbolizer);
        labelItem.setAutoWrap(100);
        painter.setLabel(labelItem);
        // should default to 0 with no lines to paint
        assertTrue(painter.getLineHeightForAnchorY(0) == 0.0);
    }

    @Test
    public void testGetLastLineHeightLabelWithAutoWrap() {
        LabelPainter painter = new LabelPainter(graphics, LabelRenderingMode.STRING);
        LabelCacheItem labelItem =
                new LabelCacheItem("LAYERID", style, shape, "line1\n\nline2", symbolizer);
        labelItem.setAutoWrap(100);
        painter.setLabel(labelItem);
        // should not default to 0
        assertTrue(painter.getLineHeightForAnchorY(0) > 0.0);

        // should get line height of first line
        assertTrue(painter.lines.get(0).getLineHeight() == painter.getLineHeightForAnchorY(1));

        // should get line height of last line
        assertTrue(
                painter.lines.get(painter.getLineCount() - 1).getLineHeight()
                        == painter.getLineHeightForAnchorY(1));
    }

    @Test
    public void testResizeGraphicWithMark2DGraphicResizeStrech() throws Exception {
        LabelCacheItem labelItem = new LabelCacheItem("LayerID", style, shape, "Test", symbolizer);
        labelItem.setGraphicsResize(LabelCacheItem.GraphicResize.STRETCH);
        Rectangle2D labelBounds = new Rectangle2D.Double(0.0, -0.6875, 0.4, 0.4);
        MarkStyle2D style2D = new MarkStyle2D();
        style2D.setShape(new Rectangle2D.Double(-0.5, -0.5, 1.0, 1.0));
        int[] graphicMargin = new int[4];
        graphicMargin[0] = 0;
        graphicMargin[1] = 0;
        graphicMargin[2] = 0;
        graphicMargin[3] = 0;
        labelItem.setGraphicMargin(graphicMargin);
        LabelPainter painter = new LabelPainter(graphics, LabelRenderingMode.OUTLINE);
        painter.setLabel(labelItem);

        Field field = painter.getClass().getDeclaredField("labelBounds");
        field.setAccessible(true);
        field.set(painter, labelBounds);

        Method method = painter.getClass().getDeclaredMethod("resizeGraphic", Style2D.class);
        method.setAccessible(true);
        Object reply = method.invoke(painter, style2D);

        // should not be null
        assertTrue(reply != null);
    }
}
